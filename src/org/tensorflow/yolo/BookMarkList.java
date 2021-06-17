package org.tensorflow.yolo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.TreeSet;

public class BookMarkList extends AppCompatActivity {
    MapFragmentActivity mapFragmentActivity;
    BackgroundWorker backgroundWorker;
    static ArrayList<String> list = new ArrayList<String>();
    static ArrayList<String> list3 = new ArrayList<String>();
    static String getListViewString;
    static int ListView = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_mark_list);

        list.clear();
        String str = backgroundWorker.markList;

        String[] array = str.split(":");

        for(int i=1 ; i< Integer.parseInt(array[0])+1; i++){
            list.add(array[i]);
        }
        TreeSet<String> list2 = new TreeSet<String>(list); //TreeSet에 list데이터 삽입
        list3 = new ArrayList<String>(list2); //중복이 제거된 HachSet을 다시 ArrayList에 삽입




        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list3) ;

        android.widget.ListView listview = (ListView) findViewById(R.id.listview1) ;
        listview.setAdapter(adapter) ;

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {

                // get TextView's Text.
                getListViewString = (String) parent.getItemAtPosition(position) ;
                ListView =1;

                mapFragmentActivity.STT.setText(mapFragmentActivity.bookMarkList.getListViewString);
                mapFragmentActivity.txtView.setText(mapFragmentActivity.bookMarkList.getListViewString);
                //mainActivity.a_d.setText("즐겨찾기 삭제");
                mapFragmentActivity.btn_add.setEnabled(false);
                mapFragmentActivity.btn_delete.setEnabled(true);
                mapFragmentActivity.bookMarkList.ListView = 0;
                finish();
            }
        }) ;
    }
}
