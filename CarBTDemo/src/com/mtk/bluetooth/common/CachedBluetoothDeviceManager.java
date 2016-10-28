/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mtk.bluetooth.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * CachedBluetoothDeviceManager manages the set of remote Bluetooth devices.
 */
public class CachedBluetoothDeviceManager {
    private static final String TAG = "CachedBluetoothDeviceManager";
    private static final boolean DEBUG = true;

    private Context mContext;
    private final List<CachedBluetoothDevice> mCachedDevices =
            new ArrayList<CachedBluetoothDevice>();

    CachedBluetoothDeviceManager(Context context) {
        mContext = context;
    }

    public synchronized Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        return new ArrayList<CachedBluetoothDevice>(mCachedDevices);
    }

    public static boolean onDeviceDisappeared(CachedBluetoothDevice cachedDevice) {
        cachedDevice.setVisible(false);
        return cachedDevice.getBondState() == BluetoothDevice.BOND_NONE;
    }

    public void onDeviceNameUpdated(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.refreshName();
        }
    }

    /**
     * Search for existing {@link CachedBluetoothDevice} or return null
     * if this device isn't in the cache. Use {@link #addDevice}
     * to create and return a new {@link CachedBluetoothDevice} for
     * a newly discovered {@link BluetoothDevice}.
     *
     * @param device the address of the Bluetooth device
     * @return the cached device object for this device, or null if it has
     *   not been previously seen
     */
    public CachedBluetoothDevice findDevice(BluetoothDevice device) {
	    Log.d(TAG,"findDevice() =="+device);
        Log.d(TAG,"findDevice() mCachedDevices size =="+mCachedDevices.size());
        for (CachedBluetoothDevice cachedDevice : mCachedDevices) {			 
			 Log.d(TAG,"findDevice() mCachedDevices =="+cachedDevice );
            if (cachedDevice.getDevice().equals(device)) {
                return cachedDevice;
            }
        }
        return null;
    }

	public CachedBluetoothDevice findDeviceByaddr(String addr) {
	    Log.d(TAG,"findDeviceByaddr() =="+addr);
		for(int i=0;i<mCachedDevices.size();i++){
			if(mCachedDevices.get(i).getDevice().getAddress().equals(addr)){
				return mCachedDevices.get(i);
			}
		}        
        return null;
    }

    /**
     * Create and return a new {@link CachedBluetoothDevice}. This assumes
     * that {@link #findDevice} has already been called and returned null.
     * @param device the address of the new Bluetooth device
     * @return the newly created CachedBluetoothDevice object
     */
    public CachedBluetoothDevice addDevice(LocalBluetoothAdapter adapter,
            LocalBluetoothProfileManager profileManager,
            BluetoothDevice device) {
            Log.d(TAG,"addDevice device =="+device);
        CachedBluetoothDevice newDevice = new CachedBluetoothDevice(mContext, adapter,
            profileManager, device); 
        synchronized (mCachedDevices) {
            mCachedDevices.add(newDevice);
        }
        return newDevice;
    }

    /**
     * Attempts to get the name of a remote device, otherwise returns the address.
     *
     * @param device The remote device.
     * @return The name, or if unavailable, the address.
     */
    public String getName(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            return cachedDevice.getName();
        }

        String name = device.getAliasName();
        if (name != null) {
            return name;
        }

        return device.getAddress();
    }

    public synchronized void clearNonBondedDevices() {
        for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
            CachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
            if (cachedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                mCachedDevices.remove(i);
                Log.d(TAG, "Clear NonBondedDevices : " + cachedDevice.getBondState());
            }
        }
    }

    public synchronized void onScanningStateChanged(boolean started) {
        if (!started) return;

        // If starting a new scan, clear old visibility
        // Iterate in reverse order since devices may be removed.
        for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
            CachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
            cachedDevice.setVisible(false);
        }
    }

    public synchronized void onBtClassChanged(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.refreshBtClass();
        }
    }

    public synchronized void onUuidChanged(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            cachedDevice.onUuidChanged();
        }
    }

    public synchronized void onBluetoothStateChanged(int bluetoothState) {
        // When Bluetooth is turning off, we need to clear the non-bonded devices
        // Otherwise, they end up showing up on the next BT enable
        if (bluetoothState == BluetoothAdapter.STATE_TURNING_OFF) {
            for (int i = mCachedDevices.size() - 1; i >= 0; i--) {
                CachedBluetoothDevice cachedDevice = mCachedDevices.get(i);
                if (cachedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    cachedDevice.setVisible(false);
                    Log.d(TAG, "Remove device for bond state : " + cachedDevice.getBondState());
                    mCachedDevices.remove(i);
                } else {
                    // For bonded devices, we need to clear the connection status so that
                    // when BT is enabled next time, device connection status shall be retrieved
                    // by making a binder call.
                    cachedDevice.clearProfileConnectionState();
                }
            }
        }
    }
    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}

