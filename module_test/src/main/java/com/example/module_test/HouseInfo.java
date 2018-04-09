package com.example.module_test;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.admistrator.Extra;

/**
 * Created by janecer on 2018/3/30 0030.
 * email:janecer@sina.cn
 */
public class HouseInfo implements Parcelable {

    public int numberCode ;

    public String address ;

    public float mile ;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.numberCode);
        dest.writeString(this.address);
        dest.writeFloat(this.mile);
    }

    @Override
    public String toString() {
        return "{address : " + address + " mile : " + mile +"}";
    }

    public HouseInfo() {
    }

    protected HouseInfo(Parcel in) {
        this.numberCode = in.readInt();
        this.address = in.readString();
        this.mile = in.readFloat();
    }

    public static final Creator<HouseInfo> CREATOR = new Creator<HouseInfo>() {
        @Override
        public HouseInfo createFromParcel(Parcel source) {
            return new HouseInfo(source);
        }

        @Override
        public HouseInfo[] newArray(int size) {
            return new HouseInfo[size];
        }
    };
}
