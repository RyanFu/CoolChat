package com.cooloongwu.coolchat.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.cooloongwu.coolchat.R;
import com.cooloongwu.coolchat.adapter.ChatAdapter;
import com.cooloongwu.coolchat.base.AppConfig;
import com.cooloongwu.coolchat.base.BaseActivity;
import com.cooloongwu.coolchat.base.MyService;
import com.cooloongwu.coolchat.entity.ChatFriend;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends BaseActivity implements View.OnClickListener {

    private ImageButton imgbtn_emoji_keyboard;
    private ImageButton imgbtn_more_send_close;
    private EditText edit_input;

    private boolean isSend = false;
    private boolean isMore = true;
    private boolean isClose = false;
    private boolean isEmoji = true;
    private boolean isKeyboard = false;

    private ArrayList<ChatFriend> listData = new ArrayList<>();
    private RecyclerView recyclerView;
    private ChatAdapter adapter;

    private MyService.MyBinder myBinder;

    private long chatId;
    private String chatType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        EventBus.getDefault().register(this);
        bindMyService();

        getData();
        initViews();
    }

    private void getData() {
        Intent intent = getIntent();
        chatId = Long.parseLong(intent.getStringExtra("chatId"));                 //好友或者群组的ID
        chatType = intent.getStringExtra("chatType");               //群组还是好友
        String chatName = intent.getStringExtra("chatName");        //群组名或者好友名
        Log.e("聊天信息", "当前在跟" + chatType + "：ID为" + chatId + "的" + chatName + "聊天");
        initToolbar(chatName);
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initViews() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(ChatActivity.this);
        //layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(ChatActivity.this, listData);
        recyclerView.setAdapter(adapter);
        edit_input = (EditText) findViewById(R.id.edit_input);
        edit_input.addTextChangedListener(textWatcher);

        imgbtn_emoji_keyboard = (ImageButton) findViewById(R.id.imgbtn_emoji_keyboard);
        imgbtn_more_send_close = (ImageButton) findViewById(R.id.imgbtn_more_send_close);
        imgbtn_emoji_keyboard.setOnClickListener(this);
        imgbtn_more_send_close.setOnClickListener(this);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    adapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    break;
                case 1:
                    Bundle bundle = msg.getData();
                    String otherMsg = bundle.getString("otherMsg");
                    Toast.makeText(ChatActivity.this, otherMsg, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Subscribe
    public void onEventMainThread(JSONObject jsonObject) {
        Log.e("聊天页面收到消息", jsonObject.toString());
        try {

            /**
             * 如果toWhich是friend，那么toId可能是自己的ID（朋友发的消息）或者朋友的Id（自己发的消息）
             *                    那么fromId可能是自己的ID（自己发的消息）或者朋友的ID（朋友发的消息）
             *
             * 如果toWhich是group，那么toId就是群组的Id
             *                   那么fromId可能是自己的ID（自己发的消息）或者群组中其他人的ID（群组中其他人发的消息）
             */
            String toWhich = jsonObject.getString("toWhich");   //可能是friend或者group
            long toId = jsonObject.getLong("toId");             //可能是我自己的ID或者对方的ID
            long fromId = jsonObject.getLong("fromId");         //可能是我自己的ID或者对方的ID

            String fromAvatar = jsonObject.getString("fromAvatar");
            String fromName = jsonObject.getString("fromName");
            String content = jsonObject.getString("content");

            if (chatType.equals(toWhich)) {
                //跟当前聊天类型匹配，是群组或者好友的消息
                if ("friend".equals(toWhich)) {
                    //当前在跟好友聊天，需判断是不是当前好友的消息
                    if ((toId == chatId && fromId == AppConfig.getUserId(ChatActivity.this)) //我发给当前朋友的消息
                            || (toId == AppConfig.getUserId(ChatActivity.this) && fromId == chatId)//朋友发给我的消息
                            ) {
                        //是跟当前好友的聊天消息
                        List<ChatFriend> chatBeens = new ArrayList<>();
                        ChatFriend chatBean = new ChatFriend();
                        chatBean.setUserId(fromId);
                        chatBean.setUserAvatar(fromAvatar);
                        chatBean.setUserName(fromName);
                        chatBean.setContent(content);
                        chatBeens.add(chatBean);
                        listData.addAll(chatBeens);

                        Message msg = new Message();
                        msg.what = 0;
                        handler.sendMessage(msg);
                    } else {
                        //不是跟当前好友的聊天消息，提示来消息了即可
                        showOtherMsg(fromName + "：" + content);
                    }
                } else {
                    //当前在跟群组聊天，需判断是不是当前群组的消息
                    if (chatId == toId) {
                        //是当前群组的聊天消息
                    } else {
                        //不是当前群组的聊天消息，提示来消息了即可
                        showOtherMsg(fromName + "：" + content);
                    }
                }
            } else {
                //跟当前聊天类型不匹配，比如当前在跟好友聊天，来的是群组消息
                showOtherMsg(fromName + "：" + content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Json解析", "出错了");
        }
    }

    private void showOtherMsg(String str) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("otherMsg", str);
        msg.setData(bundle);
        msg.what = 1;
        handler.sendMessage(msg);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgbtn_emoji_keyboard:
                //如果是“展示表情”的状态，那么点击后展示表情，按钮状态改变为“显示键盘”，并关闭键盘
                if (isEmoji) {
                    Toast.makeText(ChatActivity.this, "展示表情,隐藏键盘", Toast.LENGTH_SHORT).show();
                    imgbtn_emoji_keyboard.setImageResource(R.mipmap.conversation_btn_messages_keyboard);
                    isEmoji = false;
                    isKeyboard = true;
                    hideKeyboard();
                    return;
                }
                //如果是“展示键盘”的状态，那么点击后展示键盘，按钮状态改为“展示表情”，并展示键盘
                if (isKeyboard) {
                    Toast.makeText(ChatActivity.this, "展示键盘，隐藏表情", Toast.LENGTH_SHORT).show();
                    imgbtn_emoji_keyboard.setImageResource(R.mipmap.conversation_btn_messages_emoji);
                    isKeyboard = false;
                    isEmoji = true;
                    showKeyboard();
                    return;
                }
                break;
            case R.id.imgbtn_more_send_close:
                //如果是“展示更多”的状态，那么点击后展示更多的按钮，按钮状态改为“关闭更多”
                if (isMore) {
                    Toast.makeText(ChatActivity.this, "展示更多", Toast.LENGTH_SHORT).show();
                    imgbtn_more_send_close.setImageResource(R.mipmap.conversation_btn_messages_close);
                    isMore = false;
                    isClose = true;
                    isSend = false;
                    return;
                }
                //如果是“关闭更多”的状态，那么点击后关闭更多的按钮，按钮状态改为“展示更多”
                if (isClose) {
                    Toast.makeText(ChatActivity.this, "关闭更多", Toast.LENGTH_SHORT).show();
                    imgbtn_more_send_close.setImageResource(R.mipmap.conversation_btn_messages_more);
                    isClose = false;
                    isMore = true;
                    isSend = false;
                    return;
                }
                //如果是“发送消息”的状态，那么点击后发送消息，按钮状态改为“展示更多”状态，不关闭键盘
                if (isSend) {
                    sendMessage();
                    edit_input.setText("");
                    imgbtn_more_send_close.setImageResource(R.mipmap.conversation_btn_messages_more);
                    isSend = false;
                    isMore = true;
                    isClose = false;
                    return;
                }
                break;
            default:
                break;
        }
    }

    private void sendMessage() {
        //发送数据示例
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fromId", AppConfig.getUserId(ChatActivity.this));
            jsonObject.put("fromName", AppConfig.getUserName(ChatActivity.this));
            jsonObject.put("fromAvatar", AppConfig.getUserAvatar(ChatActivity.this));
            jsonObject.put("toWhich", chatType);
            jsonObject.put("toId", chatId);
            jsonObject.put("content", edit_input.getText().toString().trim());
            jsonObject.put("contentType", "text");

            myBinder.sendMessage(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示键盘
     */
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edit_input, InputMethodManager.SHOW_FORCED);
    }

    /**
     * 关闭键盘
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_input.getWindowToken(), 0);
    }

    /**
     * 监听输入框的变化，根据字数变化切换按钮
     */
    private TextWatcher textWatcher = new TextWatcher() {
        private CharSequence sequence;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            sequence = charSequence;
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (sequence.length() > 0) {
                imgbtn_more_send_close.setImageResource(R.mipmap.conversation_btn_messages_send);
                isSend = true;
                isMore = false;
                isClose = false;
            } else {
                imgbtn_more_send_close.setImageResource(R.mipmap.conversation_btn_messages_more);
                isSend = false;
                isMore = true;
                isClose = false;
            }
        }
    };

    /**
     * 绑定服务
     */
    private void bindMyService() {
        Intent bindIntent = new Intent(ChatActivity.this, MyService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            myBinder = (MyService.MyBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unbindService(connection);
    }


}
