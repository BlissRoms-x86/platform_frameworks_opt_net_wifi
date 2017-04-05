/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.wifi.p2p;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.test.MockAnswerUtil.AnswerWithArguments;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pWfdInfo;

import org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Unit tests for SupplicantP2pIfaceCallback
 */
public class SupplicantP2pIfaceCallbackTest {
    private static final String TAG = "SupplicantP2pIfaceCallbackTest";

    private String mIface = "test_p2p0";
    private WifiP2pMonitor mMonitor;
    private SupplicantP2pIfaceCallback mDut;

    private byte[] mDeviceAddressInvalid1 = { 0x00 };
    private byte[] mDeviceAddressInvalid2 = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66 };
    private byte[] mDeviceAddress1Bytes = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 };
    private String mDeviceAddress1String = "00:11:22:33:44:55";
    private byte[] mDeviceAddress2Bytes = { 0x01, 0x12, 0x23, 0x34, 0x45, 0x56 };
    private String mDeviceAddress2String = "01:12:23:34:45:56";
    private byte[] mDeviceInfoBytes = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };
    private static final byte[] DEVICE_ADDRESS = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

    private class SupplicantP2pIfaceCallbackSpy extends SupplicantP2pIfaceCallback {
        SupplicantP2pIfaceCallbackSpy(String iface, WifiP2pMonitor monitor) {
            super(iface, monitor);
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mMonitor = mock(WifiP2pMonitor.class);
        mDut = new SupplicantP2pIfaceCallbackSpy(mIface, mMonitor);
    }

    /**
     * Sunny day scenario for onDeviceFound call.
     */
    @Test
    public void testOnDeviceFound_success() throws Exception {
        byte[] fakePrimaryDeviceTypeBytes = { 0x01, 0x02, 0x03 };
        String fakePrimaryDeviceTypeString = "010203";
        String fakeDeviceName = "test device name";
        short fakeConfigMethods = 0x1234;
        byte fakeCapabilities = 123;
        int fakeGroupCapabilities = 456;

        doAnswer(new AnswerWithArguments() {
            public void answer(String iface, WifiP2pDevice device) {
                // NOTE: mDeviceAddress1Bytes seems to be ignored by
                // legacy implementation of WifiP2pDevice.
                assertEquals(iface, mIface);
                assertEquals(device.deviceName, fakeDeviceName);
                assertEquals(device.primaryDeviceType, fakePrimaryDeviceTypeString);
                assertEquals(device.deviceCapability, fakeCapabilities);
                assertEquals(device.groupCapability, fakeGroupCapabilities);
                assertEquals(device.wpsConfigMethodsSupported, fakeConfigMethods);
                assertEquals(device.deviceAddress, mDeviceAddress2String);
                assertEquals(device.status, WifiP2pDevice.AVAILABLE);
            }
        })
        .when(mMonitor).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));

        mDut.onDeviceFound(
                mDeviceAddress1Bytes, mDeviceAddress2Bytes,
                fakePrimaryDeviceTypeBytes,
                fakeDeviceName, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                mDeviceInfoBytes);

        mDut.onDeviceFound(
                mDeviceAddress1Bytes, mDeviceAddress2Bytes,
                fakePrimaryDeviceTypeBytes,
                fakeDeviceName, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                null);

        // Make sure we issued a broadcast each time.
        verify(mMonitor, times(2)).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));
    }

    /**
     * Failing scenarios for onDeviceFound call.
     */
    @Test
    public void testOnDeviceFound_invalidArguments() throws Exception {
        byte[] fakePrimaryDeviceTypeBytes = { 0x01, 0x02, 0x03 };
        String fakePrimaryDeviceTypeString = "010203";
        String fakeDeviceName = "test device name";
        short fakeConfigMethods = 0x1234;
        byte fakeCapabilities = 123;
        int fakeGroupCapabilities = 456;

        mDut.onDeviceFound(
                mDeviceAddress2Bytes, null,
                fakePrimaryDeviceTypeBytes,
                fakeDeviceName, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                mDeviceInfoBytes);
        verify(mMonitor, never()).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));


        mDut.onDeviceFound(
                mDeviceAddress1Bytes, mDeviceAddress2Bytes,
                null,
                fakeDeviceName, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                mDeviceInfoBytes);
        verify(mMonitor, never()).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));


        mDut.onDeviceFound(
                mDeviceAddress1Bytes, mDeviceAddress2Bytes,
                fakePrimaryDeviceTypeBytes,
                null, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                mDeviceInfoBytes);
        verify(mMonitor, never()).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));


        mDut.onDeviceFound(
                mDeviceAddress1Bytes, mDeviceAddressInvalid1,
                fakePrimaryDeviceTypeBytes,
                null, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                mDeviceInfoBytes);
        verify(mMonitor, never()).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));


        mDut.onDeviceFound(
                mDeviceAddress1Bytes, mDeviceAddressInvalid2,
                fakePrimaryDeviceTypeBytes,
                null, fakeConfigMethods,
                fakeCapabilities, fakeGroupCapabilities,
                mDeviceInfoBytes);
        verify(mMonitor, never()).broadcastP2pDeviceFound(
                anyString(), any(WifiP2pDevice.class));
    }

    /**
     * Sunny day scenario for onDeviceLost call.
     */
    @Test
    public void testOnDeviceLost_success() throws Exception {
        doAnswer(new AnswerWithArguments() {
            public void answer(String iface, WifiP2pDevice device) {
                assertEquals(iface, mIface);
                assertEquals(device.deviceAddress, mDeviceAddress1String);
                assertEquals(device.status, WifiP2pDevice.UNAVAILABLE);
            }
        })
        .when(mMonitor).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));

        mDut.onDeviceLost(mDeviceAddress1Bytes);

        // Make sure we issued a broadcast each time.
        verify(mMonitor, times(1)).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));
    }

    /**
     * Failing scenarios for onDeviceLost call.
     */
    @Test
    public void testOnDeviceLost_invalidArguments() throws Exception {
        mDut.onDeviceLost(null);
        verify(mMonitor, never()).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));

        mDut.onDeviceLost(mDeviceAddressInvalid1);
        verify(mMonitor, never()).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));

        mDut.onDeviceLost(mDeviceAddressInvalid2);
        verify(mMonitor, never()).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));
    }

    /**
     * Sunny day scenario for onGoNegotiationRequest call.
     */
    @Test
    public void testOnGoNegotiationRequest_success() throws Exception {
        HashSet<Integer> setups = new HashSet<Integer>();

        doAnswer(new AnswerWithArguments() {
            public void answer(String iface, WifiP2pConfig config) {
                assertEquals(iface, mIface);
                assertNotNull(config.wps);
                setups.add(config.wps.setup);
                assertEquals(config.deviceAddress, mDeviceAddress1String);
            }
        })
        .when(mMonitor).broadcastP2pGoNegotiationRequest(
                anyString(), any(WifiP2pConfig.class));

        mDut.onGoNegotiationRequest(mDeviceAddress1Bytes,
                (short)ISupplicantP2pIfaceCallback.WpsDevPasswordId.USER_SPECIFIED);
        assertTrue(setups.contains(WpsInfo.DISPLAY));

        mDut.onGoNegotiationRequest(mDeviceAddress1Bytes,
                (short)ISupplicantP2pIfaceCallback.WpsDevPasswordId.PUSHBUTTON);
        assertTrue(setups.contains(WpsInfo.PBC));

        mDut.onGoNegotiationRequest(mDeviceAddress1Bytes,
                (short)ISupplicantP2pIfaceCallback.WpsDevPasswordId.REGISTRAR_SPECIFIED);
        assertTrue(setups.contains(WpsInfo.KEYPAD));

        // Invalid should default to PBC
        setups.clear();
        mDut.onGoNegotiationRequest(mDeviceAddress1Bytes, (short)0xffff);
        assertTrue(setups.contains(WpsInfo.PBC));
    }

    /**
     * Failing scenarios for onGoNegotiationRequest call.
     */
    @Test
    public void testOnGoNegotiationRequest_invalidArguments() throws Exception {
        mDut.onGoNegotiationRequest(null, (short)0);
        verify(mMonitor, never()).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));

        mDut.onGoNegotiationRequest(mDeviceAddressInvalid1, (short)0);
        verify(mMonitor, never()).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));

        mDut.onGoNegotiationRequest(mDeviceAddressInvalid2, (short)0);
        verify(mMonitor, never()).broadcastP2pDeviceLost(
                anyString(), any(WifiP2pDevice.class));
    }

    /**
     * Sunny day scenario for onGroupStarted call.
     */
    @Test
    public void testOnGroupStarted_success() throws Exception {
        String fakeName = "group name";
        String fakePassphrase = "secret";
        ArrayList<Byte> fakeSsidBytesList = new ArrayList<Byte>() {{
            add((byte)0x30);
            add((byte)0x31);
            add((byte)0x32);
            add((byte)0x33);
        }};
        String fakeSsidString = "\"0123\"";
        HashSet<String> passwords = new HashSet<String>();

        doAnswer(new AnswerWithArguments() {
            public void answer(String iface, WifiP2pGroup group) {
                assertEquals(iface, mIface);
                assertNotNull(group.getOwner());
                assertEquals(group.getOwner().deviceAddress, mDeviceAddress1String);
                assertEquals(group.getNetworkId(), WifiP2pGroup.PERSISTENT_NET_ID);
                passwords.add(group.getPassphrase());
                assertEquals(group.getInterface(), fakeName);
                assertEquals(group.getNetworkName(), fakeSsidString);
            }
        })
        .when(mMonitor).broadcastP2pGroupStarted(
                anyString(), any(WifiP2pGroup.class));

        mDut.onGroupStarted(
                fakeName, true, fakeSsidBytesList, 1, null, fakePassphrase,
                mDeviceAddress1Bytes, true);
        assertTrue(passwords.contains(fakePassphrase));

        mDut.onGroupStarted(
                fakeName, true, fakeSsidBytesList, 1, null, null,
                mDeviceAddress1Bytes, true);
        assertTrue(passwords.contains(null));

        verify(mMonitor, times(2)).broadcastP2pGroupStarted(
                anyString(), any(WifiP2pGroup.class));
    }

    /**
     * Failing scenarios for onGroupStarted call.
     */
    @Test
    public void testOnGroupStarted_invalidArguments() throws Exception {
        String fakeName = "group name";
        String fakePassphrase = "secret";
        ArrayList<Byte> fakeSsidBytesList = new ArrayList<Byte>() {{
            add((byte)0x30);
            add((byte)0x31);
            add((byte)0x32);
            add((byte)0x33);
        }};
        String fakeSsidString = "0123";

        mDut.onGroupStarted(
                null, true, fakeSsidBytesList, 1, null, fakePassphrase,
                mDeviceAddress1Bytes, true);
        verify(mMonitor, never()).broadcastP2pGroupStarted(
                anyString(), any(WifiP2pGroup.class));

        mDut.onGroupStarted(
                fakeName, true, null, 1, null, fakePassphrase,
                mDeviceAddress1Bytes, true);
        verify(mMonitor, never()).broadcastP2pGroupStarted(
                anyString(), any(WifiP2pGroup.class));

        mDut.onGroupStarted(
                fakeName, true, fakeSsidBytesList, 1, null, fakePassphrase,
                null, true);
        verify(mMonitor, never()).broadcastP2pGroupStarted(
                anyString(), any(WifiP2pGroup.class));
    }

    /**
     * Test provision disovery callback.
     */
    @Test
    public void testOnProvisionDisconveryCompleted() throws Exception {
        byte[] p2pDeviceAddr = DEVICE_ADDRESS;
        boolean isRequest = false;
        byte status = ISupplicantP2pIfaceCallback.P2pProvDiscStatusCode.SUCCESS;
        short configMethods = WpsConfigMethods.DISPLAY;
        String generatedPin = "12345678";

        ArgumentCaptor<WifiP2pProvDiscEvent> discEventCaptor =
                ArgumentCaptor.forClass(WifiP2pProvDiscEvent.class);
        mDut.onProvisionDiscoveryCompleted(
                p2pDeviceAddr, isRequest, status, configMethods, generatedPin);
        verify(mMonitor).broadcastP2pProvisionDiscoveryShowPin(
                anyString(), discEventCaptor.capture());
        assertEquals(WifiP2pProvDiscEvent.SHOW_PIN, discEventCaptor.getValue().event);
        assertEquals(generatedPin, discEventCaptor.getValue().pin);

        configMethods = WpsConfigMethods.KEYPAD;
        mDut.onProvisionDiscoveryCompleted(
                p2pDeviceAddr, isRequest, status, configMethods, generatedPin);
        verify(mMonitor).broadcastP2pProvisionDiscoveryEnterPin(
                anyString(), discEventCaptor.capture());
        assertEquals(WifiP2pProvDiscEvent.ENTER_PIN, discEventCaptor.getValue().event);

        configMethods = WpsConfigMethods.PUSHBUTTON;
        mDut.onProvisionDiscoveryCompleted(
                p2pDeviceAddr, isRequest, status, configMethods, generatedPin);
        verify(mMonitor).broadcastP2pProvisionDiscoveryPbcResponse(
                anyString(), discEventCaptor.capture());
        assertEquals(WifiP2pProvDiscEvent.PBC_RSP, discEventCaptor.getValue().event);

        isRequest = true;
        mDut.onProvisionDiscoveryCompleted(
                p2pDeviceAddr, isRequest, status, configMethods, generatedPin);
        verify(mMonitor).broadcastP2pProvisionDiscoveryPbcRequest(
                anyString(), discEventCaptor.capture());
        assertEquals(WifiP2pProvDiscEvent.PBC_REQ, discEventCaptor.getValue().event);
    }
}