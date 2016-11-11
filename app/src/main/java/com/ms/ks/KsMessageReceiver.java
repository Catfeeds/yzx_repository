package com.ms.ks;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ms.entity.Order;
import com.ms.entity.OrderGoods;
import com.ms.util.BluetoothService;
import com.ms.util.CustomRequest;
import com.ms.util.LoginUtils;
import com.ms.util.PicFromPrintUtils;
import com.ms.util.PrintUtil;
import com.ms.util.QRCodeUtil;
import com.ms.util.RequestManager;
import com.ms.util.StringUtils;
import com.ms.util.SysUtils;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 1、PushMessageReceiver 是个抽象类，该类继承了 BroadcastReceiver。<br/>
 * 2、需要将自定义的 DemoMessageReceiver 注册在 AndroidManifest.xml 文件中：
 * <pre>
 * {@code
 *  <receiver
 *      android:name="com.xiaomi.mipushdemo.DemoMessageReceiver"
 *      android:exported="true">
 *      <intent-filter>
 *          <action android:name="com.xiaomi.mipush.RECEIVE_MESSAGE" />
 *      </intent-filter>
 *      <intent-filter>
 *          <action android:name="com.xiaomi.mipush.MESSAGE_ARRIVED" />
 *      </intent-filter>
 *      <intent-filter>
 *          <action android:name="com.xiaomi.mipush.ERROR" />
 *      </intent-filter>
 *  </receiver>
 *  }</pre>
 * 3、DemoMessageReceiver 的 onReceivePassThroughMessage 方法用来接收服务器向客户端发送的透传消息。<br/>
 * 4、DemoMessageReceiver 的 onNotificationMessageClicked 方法用来接收服务器向客户端发送的通知消息，
 * 这个回调方法会在用户手动点击通知后触发。<br/>
 * 5、DemoMessageReceiver 的 onNotificationMessageArrived 方法用来接收服务器向客户端发送的通知消息，
 * 这个回调方法是在通知消息到达客户端时触发。另外应用在前台时不弹出通知的通知消息到达客户端也会触发这个回调函数。<br/>
 * 6、DemoMessageReceiver 的 onCommandResult 方法用来接收客户端向服务器发送命令后的响应结果。<br/>
 * 7、DemoMessageReceiver 的 onReceiveRegisterResult 方法用来接收客户端向服务器发送注册命令后的响应结果。<br/>
 * 8、以上这些方法运行在非 UI 线程中。
 *
 * @author mayixiang
 */
public class KsMessageReceiver extends PushMessageReceiver {

    private String mRegId;
    private String mTopic;
    private String mAlias;
    private String mAccount;
    private String mStartTime;
    private String mEndTime;


    //蓝牙服务
    BluetoothService mService = null;
    //表示是否连接上蓝牙打印机
    private boolean hasConnect = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    private Order order;
    private String order_id = "";
    private Context context;
    public ArrayList<OrderGoods> goodsList;

    private MediaPlayer mediaPlayer;

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
//        Log.v("ks",
//                "onReceivePassThroughMessage is called. " + message.toString());
//        String log = context.getString(R.string.recv_passthrough_message, message.getContent());
//        MainActivity.logList.add(0, getSimpleDate() + " " + log);
//
//        if (!TextUtils.isEmpty(message.getTopic())) {
//            mTopic = message.getTopic();
//        } else if (!TextUtils.isEmpty(message.getAlias())) {
//            mAlias = message.getAlias();
//        }
//
//        Message msg = Message.obtain();
//        msg.obj = log;
//        DemoApplication.getHandler().sendMessage(msg);
    }

    @Override
    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        String content = message.getContent();
        if (content != null && content.length() > 0) {
            try {
                JSONObject a = new JSONObject(content);
                String type = a.getString("type");

                if (type.equals("new_order")) {
                    String order_id = a.getString("order_id");

                    if (!StringUtils.isEmpty(order_id)) {
                        if (LoginUtils.isSeller()) {
                            Intent intent = new Intent(context, OrderDetailActivity.class);
                            Bundle b = new Bundle();
                            b.putString("order_id", order_id);
                            intent.putExtras(b);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);

//                            Log.v("ks", "order id: " + order_id);
                        }
                    }
                } else if(type.equals("new_queue")) {
                    //消息
                    if(LoginUtils.hasLogin()) {
                        Intent intent = new Intent(context, MsgActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                } else {
                    //打开app
                    Intent intent = new Intent(context, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        String log = context.getString(R.string.click_notification_message, message.getContent());
//        MainActivity.logList.add(0, getSimpleDate() + " " + log);
//
//        if (!TextUtils.isEmpty(message.getTopic())) {
//            mTopic = message.getTopic();
//        } else if (!TextUtils.isEmpty(message.getAlias())) {
//            mAlias = message.getAlias();
//        }
//
//        Message msg = Message.obtain();
//        if (message.isNotified()) {
//            msg.obj = log;
//        }
//        DemoApplication.getHandler().sendMessage(msg);
    }

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        this.context = context;
        goodsList = new ArrayList<OrderGoods>();
        String content = message.getContent();
//        Log.v("ks", "mesage: " + message.getDescription());

        if (content != null && content.length() > 0) {
            try {
                JSONObject a = new JSONObject(content);
                String type = a.getString("type");

                if (type.equals("new_order")) {
                    order_id = a.getString("order_id");

                    if (!StringUtils.isEmpty(order_id)) {
                        //播放语音
                        final String mp3_uri = a.optString("mp3_uri");
                        Log.v("ks", "mp3: " + mp3_uri);
                        if(!TextUtils.isEmpty(mp3_uri)) {
                            Handler mHandler = new Handler(Looper.getMainLooper());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        playAudio(mp3_uri);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
//                                    playMp3(mp3_uri);
                                }
                            });
                        }
//                        initialEnv();
//                        initialTts();
//                        int result = this.mSpeechSynthesizer.speak(message.getDescription());
//                        if (result < 0) {
//                            toPrint("error,please look up error code in doc or URL:http://yuyin.baidu.com/docs/tts/122 ");
//                        }

                        if (LoginUtils.isSeller()) {
                            //消息到达时处理
                            doOrder();
//                            Log.v("ks", "order: " + order_id);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        Log.v(DemoApplication.TAG,
//                "onNotificationMessageArrived is called. " + message.toString());
//        String log = context.getString(R.string.arrive_notification_message, message.getContent());
//        MainActivity.logList.add(0, getSimpleDate() + " " + log);
//
//        if (!TextUtils.isEmpty(message.getTopic())) {
//            mTopic = message.getTopic();
//        } else if (!TextUtils.isEmpty(message.getAlias())) {
//            mAlias = message.getAlias();
//        }
//
//        Message msg = Message.obtain();
//        msg.obj = log;
//        DemoApplication.getHandler().sendMessage(msg);
    }

    @Override
    public void onCommandResult(Context context, MiPushCommandMessage message) {
//        Log.v(DemoApplication.TAG,
//                "onCommandResult is called. " + message.toString());
//        String command = message.getCommand();
//        List<String> arguments = message.getCommandArguments();
//        String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
//        String cmdArg2 = ((arguments != null && arguments.size() > 1) ? arguments.get(1) : null);
//        String log;
//        if (MiPushClient.COMMAND_REGISTER.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mRegId = cmdArg1;
//                log = context.getString(R.string.register_success);
//            } else {
//                log = context.getString(R.string.register_fail);
//            }
//        } else if (MiPushClient.COMMAND_SET_ALIAS.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mAlias = cmdArg1;
//                log = context.getString(R.string.set_alias_success, mAlias);
//            } else {
//                log = context.getString(R.string.set_alias_fail, message.getReason());
//            }
//        } else if (MiPushClient.COMMAND_UNSET_ALIAS.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mAlias = cmdArg1;
//                log = context.getString(R.string.unset_alias_success, mAlias);
//            } else {
//                log = context.getString(R.string.unset_alias_fail, message.getReason());
//            }
//        } else if (MiPushClient.COMMAND_SET_ACCOUNT.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mAccount = cmdArg1;
//                log = context.getString(R.string.set_account_success, mAccount);
//            } else {
//                log = context.getString(R.string.set_account_fail, message.getReason());
//            }
//        } else if (MiPushClient.COMMAND_UNSET_ACCOUNT.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mAccount = cmdArg1;
//                log = context.getString(R.string.unset_account_success, mAccount);
//            } else {
//                log = context.getString(R.string.unset_account_fail, message.getReason());
//            }
//        } else if (MiPushClient.COMMAND_SUBSCRIBE_TOPIC.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mTopic = cmdArg1;
//                log = context.getString(R.string.subscribe_topic_success, mTopic);
//            } else {
//                log = context.getString(R.string.subscribe_topic_fail, message.getReason());
//            }
//        } else if (MiPushClient.COMMAND_UNSUBSCRIBE_TOPIC.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mTopic = cmdArg1;
//                log = context.getString(R.string.unsubscribe_topic_success, mTopic);
//            } else {
//                log = context.getString(R.string.unsubscribe_topic_fail, message.getReason());
//            }
//        } else if (MiPushClient.COMMAND_SET_ACCEPT_TIME.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mStartTime = cmdArg1;
//                mEndTime = cmdArg2;
//                log = context.getString(R.string.set_accept_time_success, mStartTime, mEndTime);
//            } else {
//                log = context.getString(R.string.set_accept_time_fail, message.getReason());
//            }
//        } else {
//            log = message.getReason();
//        }
//        MainActivity.logList.add(0, getSimpleDate() + "    " + log);
//
//        Message msg = Message.obtain();
//        msg.obj = log;
//        DemoApplication.getHandler().sendMessage(msg);
    }

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
//        Log.v(DemoApplication.TAG,
//                "onReceiveRegisterResult is called. " + message.toString());
//        String command = message.getCommand();
//        List<String> arguments = message.getCommandArguments();
//        String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
//        String log;
//        if (MiPushClient.COMMAND_REGISTER.equals(command)) {
//            if (message.getResultCode() == ErrorCode.SUCCESS) {
//                mRegId = cmdArg1;
//                log = context.getString(R.string.register_success);
//            } else {
//                log = context.getString(R.string.register_fail);
//            }
//        } else {
//            log = message.getReason();
//        }
//
//        Message msg = Message.obtain();
//        msg.obj = log;
//        DemoApplication.getHandler().sendMessage(msg);
    }

    @SuppressLint("SimpleDateFormat")
    private static String getSimpleDate() {
        return new SimpleDateFormat("MM-dd hh:mm:ss").format(new Date());
    }

    private void doOrder() {
        //开启了自动打印
        boolean auto_print = KsApplication.getInt("auto_print", 0) == 1;
//        auto_print = true;
        if(auto_print) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null) {
                //蓝牙可用
                if (mBluetoothAdapter.isEnabled()) {
                    mService = new BluetoothService(context, mHandler);
                    if(!hasConnect) {
                        try {
                            BluetoothDevice printDev = mBluetoothAdapter.getRemoteDevice(KsApplication.getString("printer_mac", ""));

                            if(printDev != null) {
                                mService.connect(printDev);
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        //已经连接了，直接打印
                        doPrint();
                    }
                }
            }

        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            hasConnect = true;

                            doPrint();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    break;
                case BluetoothService.MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case BluetoothService.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//                    Toast.makeText(getApplicationContext(), "连接至"
//                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MESSAGE_TOAST:
                    SysUtils.showError(msg.getData().getString(BluetoothService.TOAST));
                    break;
//                case BluetoothService.MESSAGE_CONNECTION_LOST:
//                    hasConnect = false;
//                    break;
//                case BluetoothService.MESSAGE_UNABLE_CONNECT:
//                    hasConnect = false;
//                    SysUtils.showError("无法连接到配对的蓝牙打印机，请先通过打印机测试再进行收银小票打印");
//                    break;
            }
        }

    };

    public void doPrint() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("order_id", order_id);

        CustomRequest r = new CustomRequest(Request.Method.POST, SysUtils.getSellerServiceUrl("particulars"), map, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    JSONObject ret = SysUtils.didResponse(jsonObject);
                    String status = ret.getString("status");
                    String message = ret.getString("message");
                    JSONObject dataObject = ret.getJSONObject("data");

                    if (!status.equals("200")) {
                        SysUtils.showError(message);
                    } else {
                        order = SysUtils.getOrderRow(dataObject.getJSONObject("orders_info"));

                        goodsList.clear();

                        JSONArray array = dataObject.getJSONArray("orde_goods");
                        if (array != null && array.length() > 0) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject data = array.optJSONObject(i);

                                OrderGoods b = new OrderGoods();
                                b.setName(data.getString("name"));
                                b.setQuantity(data.getInt("quantity"));
                                double price = data.getDouble("price");
                                b.setAttr(data.optString("product_attr"));
//                                b.setAttr("颜色123");
                                b.setPrice(price);
                                b.setFormatPrice(SysUtils.getMoneyFormat(price));

                                goodsList.add(b);
                            }
                        }

                        //执行最终打印
                        finalDoPrint();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
        RequestManager.addRequest(r, context);

    }

    private void finalDoPrint() {
        //连接成功，开始打印，同时提交订单更新到服务器
        try {
            String index = order.getPrint_number();
            String shippingStr = order.getShippingStr2();
            String shopName = order.getSellerName();
            String deskNo = order.getDesk_num();
            String shopTel = order.getSellerTel();
            String orderDate = order.getOrderTime();
            boolean hasPay = order.getPayStatus() > 0;
            double payed = order.getPayed();
            String orderRemark = order.getMemo();
            boolean hasAddress = order.hasShippingAddr();
            String consignee = order.getShipName();
            String mobile = order.getShipMobile();
            String address = order.getShipAddr();

            String tmp = PrintUtil.getPrinterMsg(index,
                    shippingStr,
                    shopName,
                    deskNo,
                    shopTel,
                    order_id,
                    orderDate,
                    goodsList,
                    hasPay,
                    payed,
                    orderRemark,
                    hasAddress,
                    consignee,
                    mobile,
                    address);
            String printMsg = "";
            printMsg += tmp + "\n\n";

            sendMessage(printMsg);

            if(order.hasQrCode()) {
                addQrCode(order.getQrcode_url());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void addQrCode(final String uri) {
        final String filePath = QRCodeUtil.getFileRoot(context) + File.separator
                + "qr_" + System.currentTimeMillis() + ".jpg";
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = QRCodeUtil.createQRImage(uri, 360, 360, null, filePath);

                if (success) {
                    mService.printCenter();
                    sendMessage("扫码付款\n");
                    sendMessage(BitmapFactory.decodeFile(filePath));
                    sendMessage("\n\n\n");
                }
            }
        }).start();
    }

    private void sendMessage(Bitmap bitmap) {
        // Check that we're actually connected before trying anything
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            SysUtils.showError("蓝牙没有连接");
            return;
        }
        // 发送打印图片前导指令
//        byte[] start = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B,
//                0x40, 0x1B, 0x33, 0x00 };
//        mService.write(start);

        /**获取打印图片的数据**/
//		byte[] send = getReadBitMapBytes(bitmap);

        mService.printCenter();
        byte[] draw2PxPoint = PicFromPrintUtils.draw2PxPoint(bitmap);

        mService.write(draw2PxPoint);
        // 发送结束指令
//        byte[] end = { 0x1d, 0x4c, 0x1f, 0x00 };
//        mService.write(end);
    }

    private void sendMessage(String message) {
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            SysUtils.showError("蓝牙没有连接");
            return;
        }
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send;
            try {
                send = message.getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                send = message.getBytes();
            }

            mService.write(send);
        }
    }

    public void playMp3(String url){
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //播放
                    mp.start();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // dissmiss progress bar here. It will come here when
                    // MediaPlayer
                    // is not able to play file. You can show error message to user
                    return false;
                }
            });
//            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    mp.release();//释放音频资源
//                }
//            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playAudio(String url) throws Exception {
        killMediaPlayer();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(url);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void killMediaPlayer() {
        if(mediaPlayer!=null) {
            try {
                mediaPlayer.release();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
