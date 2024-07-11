package com.uda.security;

import com.uda.image.service.ImageService;
import com.uda.security.application.StatusListener;
import com.uda.security.data.*;
import com.uda.security.service.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class AppTest {
    @Mock
    private SecurityService securityService;
    @Mock
    private ImageService imageService;

    private Sensor sensor;
    private final String randomUUID = UUID.randomUUID().toString();
    @Mock
    private SecurityRepository securityRepository;

    private StatusListener statusListener;

    private Set<Sensor> getAllSensors(boolean sensorStatus, int sensorCount) {
        Set<Sensor> sensorsList = new HashSet<>();
        for (int i = 0; i < sensorCount; i++) {
            sensorsList.add(new Sensor(randomUUID, SensorType.WINDOW));
        }
        return sensorsList;
    }

    private Sensor getSensor() {
        return new Sensor(randomUUID, SensorType.WINDOW);
    }

    @BeforeEach
    protected void initData() {
        MockitoAnnotations.initMocks(this);
        securityRepository = new PretendDatabaseSecurityRepositoryImpl();
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getSensor();
        securityService.changeSensorActivationStatus(sensor, false);
    }

    @AfterEach
    protected void removeSensor() {
        securityService.removeSensor(sensor);
        var sensorsList = securityService.getSensors();
        securityService.removeAllSensors(sensorsList);
    }

    /**
     * Test case 1
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @Test
    protected void systemArm_sensorActivated_statusChangedToPending() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Assertions.assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 2
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm
     * Set the alarm status to alarm.
     */
    @Test
    protected void systemArm_sensorActivated_statusPending_systemReturnAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 3
     * If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    protected void systemStatusAlarm_sensorInactive_returnNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 4
     * If alarm is active, change in sensor state should not affect the alarm state.
     */
    @Test
    protected void activeAlarm_sensorStateChanged_systemReturnAlarm() {
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 5
     * If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    protected void sensorActive_sensorAlreadyActive_systemPending_systemReturnAlarm() {
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 6
     * If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    protected void sensorDeactivated_alreadyInactive_keepAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Assertions.assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 7
     * If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
     */
    @Test
    protected void imageContainsCat_systemArmedHome_systemReturnAlarm() {
        BufferedImage bufferedImage = new BufferedImage(2, 2, Image.SCALE_DEFAULT);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 8
     * If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    protected void imageNotContainsCat_sensorInactive_systemStatusToNoAlarm() {
        BufferedImage bufferedImage = new BufferedImage(2, 2, Image.SCALE_DEFAULT);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(false);
        securityService.processImage(bufferedImage);
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    /**
     * Test case 9
     * If the system is disarmed, set the status to no alarm.
     */
    @Test
    protected void systemDisArmed_systemStatusToNoAlarm() {
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Assertions.assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    /**
     * Testcase 10
     * If the system is armed, reset all sensors to inactive.
     */
    @Test
    protected void systemArmed_AllSensorsInactive() {
        Set<Sensor> sensorsListStart = this.getAllSensors(true, 5);
        var sensorsList = securityService.getSensors();
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        for (var sensor : sensorsList) {
            Assertions.assertEquals(false, sensor.getActive());
        }
    }

    /**
     * Testcase 11
     * If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    protected void systemArmedHome_imageContainsCat_systemStatusToAlarm() {
        BufferedImage bufferedImage = new BufferedImage(2, 2, Image.SCALE_DEFAULT);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.lenient().when(imageService.imageContainsCat(Mockito.any(), Mockito.anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        Assertions.assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }
}
