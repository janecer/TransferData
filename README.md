# TransferData
Activity之间，Activity跟Fragment之间 通过Intent获取参数传递，动态生成获取参数传递类，避免频繁写getIntent.getInt,getString,getParc...
# 生成示例代码如下
## Fragment
    @Extra(name = com.gzdxjk.ihealth.basemodule.ConstantKey.KEY_LIFE_CLUB_ID)
    String mLifeId;
    @Extra(name = ConstantKey.KEY_HOME_PRIVILEGES)
    ArrayList<HomePageInfo.PrivilegeCard> mPrivilegeCardList;//":
    @Extra(name = ConstantKey.KEY_HOME_COURSES)
    ArrayList<SuitCourseInfo> mCourseList;
    @Extra(name = ConstantKey.KEY_HOME_VIDEOS)
    ArrayList<HomePageInfo.Video> mVideoList;
## 则生成如下类
  public class HomePageServiceFragment$$Extra implements IExtra {
    @Override
    public void loadExtra(Object target, Bundle bundle) {
      HomePageServiceFragment t = (HomePageServiceFragment)target;
      bundle = bundle == null ? t.getArguments() : bundle;
      t.mLifeId = bundle.getString("life_club_id");
      t.mPrivilegeCardList = bundle.getParcelableArrayList("key_home_privileges");
      t.mCourseList = bundle.getParcelableArrayList("key_home_courses");
      t.mVideoList = bundle.getParcelableArrayList("key_home_videos");
    }

    @Override
    public void saveExtra(Object target, Bundle bundle) {
      HomePageServiceFragment t = (HomePageServiceFragment)target;
      bundle.putString("life_club_id",t.mLifeId);
      bundle.putParcelableArrayList("key_home_privileges",(java.util.ArrayList)t.mPrivilegeCardList);
      bundle.putParcelableArrayList("key_home_courses",(java.util.ArrayList)t.mCourseList);
      bundle.putParcelableArrayList("key_home_videos",(java.util.ArrayList)t.mVideoList);
    }
  }

