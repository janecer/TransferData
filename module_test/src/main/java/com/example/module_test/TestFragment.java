package com.example.module_test;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.admistrator.Extra;

import java.util.List;

/**
 * Created by janecer on 2018/4/4 0004.
 * email:janecer@sina.cn
 */
public class TestFragment extends Fragment {
    @Extra
    public String lifeClub ;

    @Extra
    public HouseInfo houseInfo ;

    @Extra
    public List<HouseInfo> houseInfoList ;

    @Extra
    public  HouseInfo[] houseInfos ;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
