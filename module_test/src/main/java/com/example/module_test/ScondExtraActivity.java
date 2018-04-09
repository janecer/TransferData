package com.example.module_test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.admistrator.Extra;
import com.example.lib_core.ExtraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by janecer on 2018/3/30 0030.
 * email:janecer@sina.cn
 */
public class ScondExtraActivity extends AppCompatActivity {

    @Extra(name = "LHName")
    public String lifeclubName ;
    @Extra(name = "LHID")
    public String lifeClubId ;
    @Extra(name = "Code")
    public int code ;
    @Extra
    public boolean isVip ;
    @Extra
    public float price ;
    @Extra
    public HouseInfo houseInfo ;
    @Extra
    public List<HouseInfo> houseInfoList ;
    @Extra
    public ArrayList<Integer> codeList ;
    @Extra
    public List<String> phones ;
    @Extra
    public HouseInfo[] infoArray ;
    @Extra
    public String abc ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_secondextra);
        ExtraManager.getInstance().loadExtras(this,savedInstanceState);
        ((TextView)findViewById(R.id.tv_extra)).setText("lifeName:" + lifeclubName + " lifeId:" + lifeClubId + "code :" + code +" isVip:" + isVip + "price :" + price +"" +
                "houseInfo : " + houseInfo + " \n houseInfoList.len : "+houseInfoList.size());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ExtraManager.getInstance().saveExtras(this,outState);
    }
}
