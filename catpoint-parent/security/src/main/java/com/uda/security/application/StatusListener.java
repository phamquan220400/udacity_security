package com.uda.security.application;

import com.uda.security.data.AlarmStatus;
import com.uda.security.data.ArmingStatus;

/**
 * Identifies a component that should be notified whenever the system status changes
 */
public interface StatusListener {
    void notify(AlarmStatus status);
    void catDetected(boolean catDetected);
    void sensorStatusChanged();
    void statusArmingChange(ArmingStatus status);
}
