    package com.arjo129.artest.device;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class CompassListener implements SensorEventListener {
    private final String TAG = "CompassListener";
    private SensorManager mSensorManager;
    private float[] mGravity = new float[3];
    public float[] mGeomagnetic = new float[3];
    public  float[] orientation = new float[3];
    public float azimuth = 0f;
    public float currentHeading = 0;
    private float correctAzimuth = 0f;
    public CompassListener(Context ctx){
        mSensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
        startListening();
    }
    public void  startListening(){
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
    }
    public void stopListenening(){
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotMatrix = new float[9];
            float[] outMatrix = new float[9];
            //ARCore uses X-Z plane as the floor vs. The device ROTATION_VECTOR x-axis is east and Y-axis is north
            SensorManager.getRotationMatrixFromVector(rotMatrix, sensorEvent.values);
            boolean succ = SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, outMatrix);
            SensorManager.getOrientation(outMatrix, orientation);
            currentHeading = (float) Math.toDegrees(orientation[0]);
            Log.d(TAG, "" + succ + " " + currentHeading);
        }

    }
    public float getBearing(){
        float eastX = mGeomagnetic[0];
        float eastZ = mGeomagnetic[2];
        // negative because positive rotation about Y rotates X away from Z
        return currentHeading;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
