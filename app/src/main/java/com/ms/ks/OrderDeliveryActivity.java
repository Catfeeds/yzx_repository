package com.ms.ks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.material.widget.PaperButton;
import com.ms.entity.Order;
import com.ms.entity.OrderGoods;
import com.ms.global.Global;
import com.ms.util.CustomRequest;
import com.ms.util.StringUtils;
import com.ms.util.SysUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OrderDeliveryActivity extends BaseActivity {
    private ListView lv_content;

    private View layout_err, include_nowifi, include_noresult;
    private Button load_btn_refresh_net, load_btn_retry;
    private TextView load_tv_noresult;
    private int textColor, redColor;
    public ArrayList<OrderGoods> cat_list;

    private Order order;


    public TextView textView3, textView10, textView2, textView4, textView5, textView6, textView7;
    public RelativeLayout relativeLayout1;
    public LinearLayout linearLayout5;
    public TextView editText1, editText2;

    public TextView shipKuaidi, shipWaimai;

    public View shipView;

    private int corp_id = 0;
    private String corp_name = "";
    private String corp_no = "";

    public TextView corp_name_label;
    public TextView corp_no_label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        initToolbar(this);

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("order")) {
                order = bundle.getParcelable("order");
            }
        }

        textColor = getResources().getColor(R.color.text_color);
        redColor = getResources().getColor(R.color.red_color);

        layout_err = (View) findViewById(R.id.layout_err);
        include_noresult = layout_err.findViewById(R.id.include_noresult);
        load_btn_retry = (Button) layout_err.findViewById(R.id.load_btn_retry);
        load_btn_retry.setVisibility(View.GONE);
        load_tv_noresult = (TextView) layout_err.findViewById(R.id.load_tv_noresult);
        load_tv_noresult.setText("订单不存在");
        load_tv_noresult.setCompoundDrawablesWithIntrinsicBounds(
                0, //left
                R.drawable.icon_no_result_melt, //top
                0, //right
                0//bottom
        );
        include_nowifi = layout_err.findViewById(R.id.include_nowifi);
        load_btn_refresh_net = (Button) include_nowifi.findViewById(R.id.load_btn_refresh_net);
        load_btn_refresh_net.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //重新加载数据
                initView();
            }
        });

        cat_list = new ArrayList<OrderGoods>();

        lv_content = (ListView) findViewById(R.id.lv_content);
        View firstView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.item_order, lv_content, false);
        lv_content.addHeaderView(firstView);

        shipView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.item_order_ship, lv_content, false);
        lv_content.addFooterView(shipView);
        shipView.setVisibility(View.GONE);

        shipKuaidi = (TextView) shipView.findViewById(R.id.editText1);
        shipWaimai = (TextView) shipView.findViewById(R.id.editText2);

        textView3 = (TextView) firstView.findViewById(R.id.textView3);
        textView10 = (TextView) firstView.findViewById(R.id.textView10);
        textView2 = (TextView) firstView.findViewById(R.id.textView2);
        textView4 = (TextView) firstView.findViewById(R.id.textView4);
        textView5 = (TextView) firstView.findViewById(R.id.textView5);
        relativeLayout1 = (RelativeLayout) firstView.findViewById(R.id.relativeLayout1);
        textView6 = (TextView) firstView.findViewById(R.id.textView6);
        textView7 = (TextView) firstView.findViewById(R.id.textView7);

        linearLayout5 = (LinearLayout) firstView.findViewById(R.id.linearLayout5);
        editText1 = (TextView) firstView.findViewById(R.id.editText1);
        editText2 = (TextView) firstView.findViewById(R.id.editText2);


        View shipView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.delivery_op, lv_content, false);
        lv_content.addHeaderView(shipView);

        //选择物流公司
        View set_ship = (View) shipView.findViewById(R.id.set_ship_item);
        corp_name_label = (TextView) set_ship.findViewById(R.id.ll_set_hint_text);
        SysUtils.setLine(set_ship, Global.SET_CELLUP, "选择物流公司", 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SysUtils.startAct(OrderDeliveryActivity.this, new DeliveryCorpActivity(), null, true);
            }
        });

        //输入快递单号
        View set_no = (View) shipView.findViewById(R.id.set_no_item);
        corp_no_label = (TextView) set_no.findViewById(R.id.ll_set_hint_text);
        SysUtils.setLine(set_no, Global.SET_SINGLE_LINE, "输入快递单号", 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putString("code", corp_no);

                SysUtils.startAct(OrderDeliveryActivity.this, new DeliveryCodeActivity(), b, true);

            }
        });

        PaperButton button1 = (PaperButton) shipView.findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (corp_id <= 0) {
                    SysUtils.showError("请选择物流公司");
                } else if (StringUtils.isEmpty(corp_no)) {
                    SysUtils.showError("请填写快递单号");
                } else {
                    Map<String,String> map = new HashMap<String,String>();
                    map.put("order_id", order.getOrderSn());
                    map.put("corp_id", String.valueOf(corp_id));
                    map.put("logi_no", corp_no);

                    CustomRequest r = new CustomRequest(Request.Method.POST, SysUtils.getSellerServiceUrl("ship_status"), map, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            hideLoading();

                            try {
                                JSONObject ret = SysUtils.didResponse(jsonObject);
                                String status = ret.getString("status");
                                String message = ret.getString("message");

                                if (!status.equals("200")) {
                                    SysUtils.showError(message);
                                } else {
                                    SysUtils.showSuccess("订单已发货");

                                    sendBroadcast(new Intent(Global.BROADCAST_REFRESH_ORDER_DETAIL_ACTION)
                                            .putExtra("order_id", order.getOrderSn()));

                                    finish();
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            hideLoading();

                            SysUtils.showNetworkError();
                        }
                    });

                    executeRequest(r);

                    showLoading(OrderDeliveryActivity.this, "请稍等......");
                }
            }
        });

        lv_content.setAdapter(null);

        initView();
    }

    private void initView() {
        textView3.setText(order.getOrderTime());
        textView10.setText(order.getPayStatusStr());

        if (order.getPayStatus() == 1) {
            textView10.setTextColor(textColor);
        } else {
            textView10.setTextColor(redColor);
        }

        if (order.hasShippingAddr()) {
            textView2.setText("配送地址：" + order.getShipAddr());
            textView2.setVisibility(View.VISIBLE);
        } else {
            textView2.setVisibility(View.GONE);
        }
        textView4.setText(order.getShippingStr());
        textView5.setText("订单号：" + order.getOrderSn());

        if (order.getPayed() > 0 || !StringUtils.isEmpty(order.getName())) {
            if (!StringUtils.isEmpty(order.getName())) {
                textView6.setText("下单用户：" + order.getName());
            } else {
                textView6.setText("");
            }

            if (order.getPayed() > 0) {
                textView7.setText(SysUtils.getMoneyFormat(order.getPayed()));
            } else {
                textView7.setText("");
            }
            relativeLayout1.setVisibility(View.VISIBLE);
        } else {
            relativeLayout1.setVisibility(View.GONE);
        }

        linearLayout5.setVisibility(View.GONE);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Bundle b = data.getExtras();
            if(b != null) {
                if (b.containsKey("corp_id") && b.containsKey("corp_name")) {
                    corp_id = b.getInt("corp_id");
                    corp_name = b.getString("corp_name");

                    updateView();
                } else if (b.containsKey("corp_code")) {
                    corp_no = b.getString("corp_code");

                    updateView();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateView() {
        corp_name_label.setText(corp_name);
        corp_no_label.setText(corp_no);
    }
}