package com.example.administrator;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.admistrator.Extra;
import com.example.module_test.HouseInfo;
import com.example.module_test.ScondExtraActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by janecer on 2018/3/27 0027.
 * email:janecer@sina.cn
 */
public class MainActivity extends AppCompatActivity {

    @Extra(name = "hello")
    public String test ;

    @Override
    protected void onCreate(@Nullable Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HouseInfo info1 = new HouseInfo() ;
                info1.mile = 11 ;
                info1.address = "天河区思成路" ;
                HouseInfo info2 = new HouseInfo() ;
                info1.mile = 11 ;
                info1.address = "天河区思成路" ;
                ArrayList<HouseInfo> infoList = new ArrayList<>() ;
                infoList.add(info1) ;
                infoList.add(info2) ;

                HouseInfo[] infoArray = {info1,info2} ;
                Bundle bundle = new Bundle() ;
                bundle.putParcelableArray("infoArray",infoArray);
                Intent intent = new Intent(MainActivity.this, ScondExtraActivity.class) ;
                intent.putExtra("LHName","广州测试馆").putExtra("LHID","000001").putExtra("Code","123").putExtra("isVip",true).putExtra("price",88.1f)
                .putExtra("houseInfo",info1).putParcelableArrayListExtra("houseInfoList",infoList);
                startActivity(intent);
            }
        });
    }
}
