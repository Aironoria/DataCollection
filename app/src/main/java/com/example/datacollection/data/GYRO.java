package com.example.datacollection.data;

public class GYRO {
    private double gx;
    private double gy;
    private double gz;

    @Override
    public String toString() {
        return  gx +","+ gy +","+ gz;
    }

    public GYRO(double gx, double gy, double gz) {
        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
    }

    public double getGx() {
        return gx;
    }

    public void setGx(double gx) {
        this.gx = gx;
    }

    public double getGy() {
        return gy;
    }

    public void setGy(double gy) {
        this.gy = gy;
    }

    public double getGz() {
        return gz;
    }

    public void setGz(double gz) {
        this.gz = gz;
    }
}
