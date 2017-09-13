package com.ble;import android.app.Service;import android.bluetooth.BluetoothDevice;import android.bluetooth.BluetoothGatt;import android.bluetooth.BluetoothGattCallback;import android.bluetooth.BluetoothGattCharacteristic;import android.bluetooth.BluetoothGattDescriptor;import android.bluetooth.BluetoothGattService;import android.bluetooth.BluetoothProfile;import android.content.Intent;import android.os.Binder;import android.os.IBinder;import android.util.Log;import com.util.Byte2HexUtil;import java.util.ArrayList;import java.util.List;import java.util.UUID;import static com.ble.BTTempDevice.SERVER_UUID;import static com.ble.BTTempDevice.Temp_SendCharateristicUUID;/* * 管理蓝牙的服务功能： *			    1) 连接蓝牙设备 *				2) 管理连接状态 *				3) 获取蓝牙设备的相关服务  * @author Kevin.wu *  */public final class BTTempBLEService extends Service {    private static final UUID NOTIY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");    public final static String ACTION_GATT_CONNECTED = "com.rfstar.kevin.service.ACTION_GATT_CONNECTED";    public final static String ACTION_GATT_CONNECTING = "com.rfstar.kevin.service.ACTION_GATT_CONNECTING";    public final static String ACTION_GATT_DISCONNECTED = "com.rfstar.kevin.service.ACTION_GATT_DISCONNECTED";//断开    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.rfstar.kevin.service.ACTION_GATT_SERVICES_DISCOVERED";    public final static String ACTION_DATA_AVAILABLE = "com.rfstar.kevin.service.ACTION_DATA_AVAILABLE";//特征值变化    public final static String ACTION_LOCK_STARTS = "com.rfstar.kevin.service.ACTION_LOCK_STARTS";//锁状态    public final static String ACTION_TEMP_UPDATE = "com.rfstar.kevin.service.ACTION_TEMP_UPDATE";//温度更新    public final static String ACTION_HUM_UPDATE = "com.rfstar.kevin.service.ACTION_HUM_UPDATE";//湿度更新    public final static String EXTRA_DATA = "com.rfstar.kevin.service.EXTRA_DATA";    public final static String ACTION_GAT_RSSI = "com.rfstar.kevin.service.RSSI";    public final static String ACTION_BIND_MAC = "com.rfstar.kevin.service.BIND_MAC";    public final static String RFSTAR_CHARACTERISTIC_ID = "com.rfstar.kevin.service.characteristic"; // 唯一标识，发送带蓝牙信息的广播    public final static String ACTION_TEN_MINUTES = "ACTION_TEN_MINUTES"; // 唯一标识，发送带蓝牙信息的广播    public static final String ACTION_DATA_ITEMFRAGMENT = "ACTION_DATA_ITEMFRAGMENT";//ACTION_DATA_ITEMFRAGMENT    private final IBinder kBinder = new LocalBinder();    private static ArrayList<BluetoothGatt> arrayGatts = new ArrayList<BluetoothGatt>(); // 存放BluetoothGatt的集合    @Override    public IBinder onBind(Intent intent) {        Log.i("BTTempBLEService", "bind ok");        return kBinder;    }    @Override    public boolean onUnbind(Intent intent) {        return super.onUnbind(intent);    }    /**     * 初始化BLE 如果已经连接就不用再次连     *     * @param device     * @return     */    public boolean initBluetoothDevice(final BluetoothDevice device) {        BluetoothGatt gatt = this.getBluetoothGatt(device);        if (gatt != null) {            gatt.close();            arrayGatts.remove(gatt);            gatt = null;        }        Log.e("BTTempBLEService", "gatt: 蓝牙设备正准备连接" + device.getAddress());        gatt = device.connectGatt(this, false, bleGattCallback);        arrayGatts.add(gatt);        if (gatt != null) {            Log.e("BTTempBLEService", "gatt: 蓝牙设备正准备连接1==" + gatt);        }        return true;    }    /**     * 断开所有连接     */    public void disconnect() {        ArrayList<BluetoothGatt> gatts = new ArrayList<BluetoothGatt>();        for (BluetoothGatt gatt : arrayGatts) {            gatts.add(gatt);            gatt.disconnect();            if (gatt != null) {                gatt.close();            }        }        arrayGatts.removeAll(gatts);    }    /**     * 根据设备的Mac地址断开连接     *     * @param address     */    public void disconnect(String address) {        ArrayList<BluetoothGatt> gatts = new ArrayList<BluetoothGatt>();        for (BluetoothGatt gatt : arrayGatts) {            if (gatt.getDevice().getAddress().equals(address)) {                Log.e("BTTempBLEService", " 根据设备的Mac地址断开连接=======1");                arrayGatts.remove(gatt);                gatts.add(gatt);                gatt.disconnect();                if (gatt != null) {                    gatt.close();                }                gatt = null;                arrayGatts.removeAll(gatts);            }        }    }    public class LocalBinder extends Binder {        public BTTempBLEService getService() {            return BTTempBLEService.this;        }    }    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {        /*         * 连接的状发生变化 (non-Javadoc)         * 当调用了连接函数 mBluetoothGatt =  bluetoothDevice.connectGatt(this.context, false, gattCallback);之后，         *	如果连接成功就会 走到 连接状态回调：         *         * @see         * android.bluetooth.BluetoothGattCallback#onConnectionStateChange(android         * .bluetooth.BluetoothGatt, int, int)         */        @Override        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {            String intentAction;            if (newState == BluetoothProfile.STATE_CONNECTED) {                broadcastUpdate(ACTION_GATT_CONNECTED, gatt.getDevice());                Log.e("BTTempBLEService", "已连接");                // Attempts to discover services after successful connection.	寻找服务                boolean b = gatt.discoverServices();                Log.e("BTTempBLEService", "b:" + b);            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {                for (BluetoothGatt gatt_1 : arrayGatts) {                    if (gatt_1.getDevice().getAddress().equals(gatt.getDevice().getAddress())) {                        Log.e("BTTempBLEService", "Disconnected from GATT server.=====" + gatt.getDevice().getAddress());                        arrayGatts.remove(gatt_1);                        gatt_1 = null;                        break;                    }                }                intentAction = ACTION_GATT_DISCONNECTED;                Log.e("BTTempBLEService", "=======蓝牙断开连接==========" + gatt.getDevice().getAddress());                gatt.close();                broadcastUpdate(intentAction, gatt.getDevice());            }        }        /*         * 搜索device中的services (non-Javadoc)         * 当判断到连接成功之后，会去寻找服务， 这个过程是异步的，会耗点时间，当寻找到服务之后，会走到回调：         * @see         * android.bluetooth.BluetoothGattCallback#onServicesDiscovered(android         * .bluetooth.BluetoothGatt, int)         */        @Override        public void onServicesDiscovered(BluetoothGatt gatt, int status) {            Log.e("BTTempBLEService", "onServicesDiscovered status=" + status);            if (status == BluetoothGatt.GATT_SUCCESS) {                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, gatt.getDevice());                // 寻找到服务                // 寻找服务之后，我们就可以和设备进行通信，比如下发配置值(写数据)，获取设备电量什么的                for (BluetoothGattService service : gatt.getServices()) {                    Log.w("BTTempBLEService", service.getUuid().toString());                }            } else if (status == 129) {                Log.e("BTTempBLEService", "onServicesDiscovered received: status=" + status);            } else {                Log.e("BTTempBLEService", "onServicesDiscovered received: status= " + status);            }        }        /*         * 读取特征值(non-Javadoc)         *  如果读取电量（或者读取其他值）成功之后 ，会来到回调：         * @see         * android.bluetooth.BluetoothGattCallback#onCharacteristicRead(android         * .bluetooth.BluetoothGatt,         * android.bluetooth.BluetoothGattCharacteristic, int)         */        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {            // 读取到值,根据UUID来判断读到的是什么值            if (status == BluetoothGatt.GATT_SUCCESS) {                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, gatt.getDevice().getAddress());                Log.i("BTTempBLEService", "onCharacteristicRead received: " + status + "-->" + Byte2HexUtil.byte2Hex(characteristic.getValue()));            } else {                Log.i("BTTempBLEService", "onCharacteristicRead false" + status + "-->" + characteristic.getUuid().toString());                //onCharacteristicRead false133-->android.bluetooth.BluetoothGattCharacteristic@42ad96c8            }        }        /*         * 如果下发配置(写数据)成功之后，会来到回调：         * (non-Javadoc)         * @see android.bluetooth.BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)         */        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {            // write成功（发送值成功），可以根据 characteristic.getValue()来判断是哪个值发送成功了，            // 比如 连接上设备之后你有一大串命令需要下发，你调用多次写命令， 这样你需要判断是不是所有命令都成功了，            // 因为android不太稳定，有必要来check命令是否成功，否则你会发现你明明调用 写命令，但是设备那边不响应            //			Log.i("onwrite",gatt.getDevice().getAddress()+":"+Byte2HexUtil.byte2Hex(characteristic.getValue()));            //			readValue(gatt.getDevice(), characteristic);            Log.e("BTTempBLEService", "onCharacteristicWrite status=" + status);        }        /*         *         * (non-Javadoc)         * @see android.bluetooth.BluetoothGattCallback#onDescriptorWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattDescriptor, int)         */        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {            Log.e("BTTempBLEService", "onDescriptorWrite status=" + status);            if (status == BluetoothGatt.GATT_SUCCESS) {                BTTempDevice.mHandler2.sendEmptyMessage(11);            }        }        /*         * 特征值的变化 (non-Javadoc)         * @see         * android.bluetooth.BluetoothGattCallback#onCharacteristicChanged(android.bluetooth.BluetoothGatt,android.bluetooth.BluetoothGattCharacteristic)          *         */        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {            // 收到设备notify值 （设备上报值），根据 characteristic.getUUID()来判断是谁发送值给你，根据 characteristic.getValue()来获取这个值            Log.d("BTTempBLEService", "onCharacteristicChanged  : " + gatt.getDevice().getAddress() + ",   " + characteristic.getUuid().toString());            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, gatt.getDevice().getAddress());            Log.d("BTTempBLEService", "接收到的数据 b  : " + Byte2HexUtil.byte2Hex(characteristic.getValue()));            if (characteristic.getUuid().toString().contains(BTTempDevice.Temp_ReceiveCharateristicUUID)) {                byte[] b = characteristic.getValue();                if (b != null && b.length > 4) {                    if (b[0] == (byte) 0xcc && b[1] == (byte) 0x0a) {//获取温湿度                        switch (b[2]) {                            case (byte) 0x1a:                                int isLock = b[3] & 0x01;                                Intent intent = new Intent(ACTION_LOCK_STARTS);                                intent.putExtra(EXTRA_DATA, characteristic.getValue());                                intent.putExtra(RFSTAR_CHARACTERISTIC_ID, characteristic.getUuid().toString());                                intent.putExtra("mac", gatt.getDevice().getAddress());                                if (isLock == 0) {//开锁失败                                    intent.putExtra("startus", false);                                } else if (isLock == 1) {//开锁成功                                    intent.putExtra("startus", true);                                }                                sendBroadcast(intent);                                break;                            case (byte) 0x1b: //获取温湿度                                Intent intent_temp = new Intent(ACTION_TEMP_UPDATE);                                intent_temp.putExtra(EXTRA_DATA, characteristic.getValue());                                intent_temp.putExtra(RFSTAR_CHARACTERISTIC_ID, characteristic.getUuid().toString());                                intent_temp.putExtra("mac", gatt.getDevice().getAddress());                                intent_temp.putExtra("temp", (int) b[4] + "." + (int) b[5]);                                intent_temp.putExtra("hum", (int) b[6] + "." + (int) b[7]);                                sendBroadcast(intent_temp);                                Log.d("BTTempBLEService", "temp: " + (int) b[4] + "." + (int) b[5]);                                break;                            case (byte) 0x1d://获取锁的状态                                int lockStaus = b[3] & 0x01;                                Intent intent_starts = new Intent(ACTION_LOCK_STARTS);                                intent_starts.putExtra(EXTRA_DATA, characteristic.getValue());                                intent_starts.putExtra(RFSTAR_CHARACTERISTIC_ID, characteristic.getUuid().toString());                                intent_starts.putExtra("mac", gatt.getDevice().getAddress());                                if (lockStaus == 0) {//锁关闭                                    intent_starts.putExtra("startus", false);                                } else if (lockStaus == 1) {//锁打开                                    intent_starts.putExtra("startus", true);                                }                                sendBroadcast(intent_starts);                                break;                            default:                                break;                        }                    }                }            }        }        /*         * 读取信号 (non-Javadoc)         *         * @see         * android.bluetooth.BluetoothGattCallback#onReadRemoteRssi(android.         * bluetooth.BluetoothGatt, int, int)         */        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {            Log.d("BTTempBLEService", "onReadRemoteRssi  : " + gatt.getDevice().getAddress() + "," + rssi);            Intent intent = new Intent(ACTION_GAT_RSSI);            intent.putExtra("mac", gatt.getDevice().getAddress());            intent.putExtra("rssi", rssi);            sendBroadcast(intent);        }    };    /**     * 广播     *     * @param action     */    private void broadcastUpdate(String action, BluetoothDevice device) {        Intent intent = new Intent(action);        intent.putExtra("BT-MAC", device.getAddress());        intent.putExtra("name", device.getName());        sendBroadcast(intent);    }    /**     * 发送带蓝牙信息的广播     *     * @param action     * @param characteristic     */    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic, String mac) {        Intent intent = new Intent(action);        // For all other profiles, writes the data formatted in HEX.        final byte[] data = characteristic.getValue();        if (data != null && data.length > 0) {            intent.putExtra(EXTRA_DATA, characteristic.getValue());            intent.putExtra(RFSTAR_CHARACTERISTIC_ID, characteristic.getUuid().toString());            intent.putExtra("BT-MAC", mac);        }        sendBroadcast(intent);    }    /**     * 读取设备数据     *     * @param device     * @param characteristic     */    public void readValue(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {        // TODO Auto-generated method stub        BluetoothGatt gatt = this.getBluetoothGatt(device);        Log.i("BTTempBLEService", "readValue gatt:" + gatt);        if (gatt == null) {            Log.w("BTTempBLEService", "readValue gatt is null");//            return;        }        boolean b = gatt.readCharacteristic(characteristic);        Log.e("BTTempBLEService", "readValue  readCharacteristic result:" + b);    }    /**     * 写入设备数据，一个Characteristic包含一个Value和多个Descriptor，一个Descriptor包含一个Value     *     * @param device     * @param characteristic     */    public void writeValue(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {        // TODO Auto-generated method stub        BluetoothGatt gatt = this.getBluetoothGatt(device);        if (gatt == null) {            Log.e("BTTempBLEService", "writeValue: 	kBluetoothGatt 没有初始化，所以不能写入数据");            return;        }        if (characteristic.getValue() == null) {            Log.e("BTTempBLEService", "特征值为0 所以不能写入数据");            return;        }        BluetoothGattCharacteristic characteristic2 = gatt.getService(UUID.fromString("0000"+SERVER_UUID+"-0000-1000-8000-00805f9b34fb"))                .getCharacteristic(UUID.fromString("0000"+Temp_SendCharateristicUUID+"-0000-1000-8000-00805f9b34fb"));        //设置要写的值        characteristic2.setValue(characteristic.getValue());        //characteristic2.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);//在Write 操作的时候，这个办法可以增快下发速度，且不需要加sleep        //写        boolean write = gatt.writeCharacteristic(characteristic2);        Log.e("BTTempBLEService", "address:" + device.getAddress() + "	write：" + write);        try {            Thread.sleep(10);        } catch (InterruptedException e) {            // TODO Auto-generated catch block            e.printStackTrace();        }    }    //读取消息    public void setCharacteristicNotification(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean enable) {        // TODO Auto-generated method stub        BluetoothGatt gatt = this.getBluetoothGatt(device);        if (gatt == null) {            Log.e("BTTempBLEService", "kBluetoothGatt 为没有初始化，所以不能发送使能数据");            return;        }        boolean b1 = gatt.setCharacteristicNotification(characteristic, enable);        Log.d("BTTempBLEService", "characteristic:" + characteristic.getUuid().toString() + "	setCharacteristicNotification result:" + b1);        try {            Thread.sleep(100);        } catch (InterruptedException e) {            // TODO Auto-generated catch block            e.printStackTrace();        }        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();        for (BluetoothGattDescriptor descriptor : descriptors) {            Log.d("BTTempBLEService", "descriptor: " + descriptor.getUuid().toString());        }        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIY);        if (descriptor != null) {            Log.d("BTTempBLEService", "开始写入BluetoothGattDescriptor");            boolean b = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);            Log.d("BTTempBLEService", "BluetoothGattDescriptor result:" + b);            gatt.writeDescriptor(descriptor);        } else {            Log.e("BTTempBLEService", "descriptor is null");        }    }    /**     * 获取services     *     * @return     */    public List<BluetoothGattService> getSupportedGattServices(BluetoothDevice device) {        BluetoothGatt gatt = this.getBluetoothGatt(device);        if (gatt == null) {            Log.w("BTTempBLEService", "  services is null ");            return null;        }        Log.w("BTTempBLEService", "  services is not null ");        return gatt.getServices();    }    /**     * 根据设备的Mac地址从已经连接的设备中匹配对应的BluetoothGatt对象     *     * @param device     * @return     */    private BluetoothGatt getBluetoothGatt(BluetoothDevice device) {        BluetoothGatt gatt = null;        for (BluetoothGatt tmpGatt : arrayGatts) {//已经连接上的对象            if (tmpGatt.getDevice().getAddress().equals(device.getAddress())) {                gatt = tmpGatt;            }            Log.d("BTTempBLEService", "	tmpGatt Address :" + tmpGatt.getDevice().getAddress());        }        return gatt;    }    /**     * 读取信号     *     * @param device     */    public void readRssi(BluetoothDevice device) {        BluetoothGatt gatt = this.getBluetoothGatt(device);        if (gatt == null) return;        boolean b = gatt.readRemoteRssi();    }}