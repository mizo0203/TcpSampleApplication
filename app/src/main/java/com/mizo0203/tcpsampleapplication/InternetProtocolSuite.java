package com.mizo0203.tcpsampleapplication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/* package */ class InternetProtocolSuite {
    private static final int PORT = 8888;

    /**
     * Based on Mr. youten source code. https://qiita.com/youten_redo/items/9e5c2afa59b68d363500
     *
     * <blockquote>
     *
     * 参考：http://stackoverflow.com/questions/2993874/android-broadcast-address
     * このメソッドは見つかった順に返すため、WiFi側に限定したい際には AndroidのAPI側WiFiManagerあたりから取得する必要がある。
     * 参考：https://code.google.com/p/boxeeremote/wiki/AndroidUDP
     *
     * </blockquote>
     */
    private static String getBroadcastAddress() {
        try {
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
                    niEnum.hasMoreElements(); ) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress != null) {
                            InetAddress broadcastAddress = interfaceAddress.getBroadcast();
                            if (broadcastAddress != null) {
                                return broadcastAddress.toString().substring(1);
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            // ignore;
        }
        return null;
    }

    /**
     * Based on Mr. youten source code. https://qiita.com/youten_redo/items/9e5c2afa59b68d363500
     */
    private static void mySleep(@SuppressWarnings("SameParameterValue") long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // ignore;
        }
    }

    /**
     * Based on Mr. youten source code. https://qiita.com/youten_redo/items/9e5c2afa59b68d363500
     */
    /* package */ void broadcast(@SuppressWarnings("SameParameterValue") String text) throws Exception {
        // PC（JDK）環境だとこの設定有無で返ってくるbroadcastAddressが変化する。
        // System.setProperty("java.net.preferIPv4Stack", "true");

        // ブロードキャストアドレスは正確に記載する必要がある。
        // 送信時に例外になったり受信できなかったりする。
        String broadcastAddress = getBroadcastAddress();
        System.out.println("broadcast=" + broadcastAddress);

        final CountDownLatch latch = new CountDownLatch(1);
        final int COUNT = 5; // 5回 "Hello"を送信

        // 受信側スレッド
        new Thread(
                        () -> {
                            try {
                                DatagramChannel recvCh = DatagramChannel.open();
                                // recvCh.configureBlocking(true); 初期値がtrueなので呼ぶ必要はない。
                                // recvCh.socket().setBroadcast(true); 受信側には不要、呼ぶ必要はない。
                                // recvCh.socket().setReuseAddress(true); 呼ぶ必要はない模様。
                                recvCh.socket().bind(new InetSocketAddress(PORT));
                                // ここでconnect()してはダメ。NotYetConnectedExceptionも本件には関係ない。

                                ByteBuffer recvBuf = ByteBuffer.allocate(1024);
                                for (int i = 0; i < COUNT; i++) {
                                    // ここでreceive()じゃなくてread()するのは間違い。
                                    recvCh.receive(recvBuf);
                                    System.out.println("receive");
                                    recvBuf.flip();
                                    int limit = recvBuf.limit();
                                    System.out.println("limit=" + limit);
                                    String hello =
                                            new String(
                                                    recvBuf.array(),
                                                    recvBuf.position(),
                                                    limit,
                                                    StandardCharsets.UTF_8);
                                    System.out.println("hello=" + hello);
                                    recvBuf.clear();
                                }

                                latch.countDown();
                                recvCh.close();
                                System.out.println("recvCh close");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        })
                .start();

        // 送信側
        DatagramChannel sendCh = DatagramChannel.open();
        // recvCh.configureBlocking(true); 初期値がtrueなので呼ぶ必要はない。
        // recvCh.socket().setReuseAddress(true); 送信側は関係ないので不要。
        // JDKでは不要だったがAndroidでは必要、SocketException EACCESが発生する。
        sendCh.socket().setBroadcast(true);

        // ByteBuffer#allocate()+put()の場合は送信前にflip()が必要だが、
        // wrap()はflip()が不要なことに注意（flip()済みという言い方が正しいかも）
        ByteBuffer sendBuf = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        InetSocketAddress portISA = new InetSocketAddress(broadcastAddress, PORT);
        for (int i = 0; i < COUNT; i++) {
            mySleep(200);
            sendCh.send(sendBuf, portISA);
            System.out.println("send");
            sendBuf.clear();
        }
        sendCh.close();
        System.out.println("sendCh close");

        latch.await(30, TimeUnit.SECONDS);
        System.out.println("latch.getCount()=" + latch.getCount());
    }
}
