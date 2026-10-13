package com.oritong.cloudstorage.entity;

public interface BalloonFlyer {

    default boolean stopFlying(){
        return false;
    }
}
