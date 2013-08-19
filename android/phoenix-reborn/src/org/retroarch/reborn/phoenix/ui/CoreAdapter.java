package org.retroarch.reborn.phoenix.ui;

import java.util.ArrayList;

import org.retroarch.reborn.phoenix.R;
import org.retroarch.reborn.phoenix.data.CoreVO;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CoreAdapter extends ArrayAdapter<CoreVO> {

	private Context context;

	public CoreAdapter(Context context, ArrayList<CoreVO> values) {
		super(context, R.layout.core_list_item, values);
		this.context = context;
	}
	
	
	
	@Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    CoreVO item = getItem(position);
	    View rowView;
	    if(item.type == CoreVO.TYPE_TITLE){
	    	rowView = inflater.inflate(R.layout.core_list_title, parent, false);
	    	TextView titleView = (TextView) rowView.findViewById(R.id.coreListTitle);
	    	titleView.setText(item.displayName);
	    	return rowView;
	    }else{
	    	rowView = inflater.inflate(R.layout.core_list_item, parent, false);
	    	TextView nameView = (TextView) rowView.findViewById(R.id.coreName);
	    	TextView systemNameView = (TextView) rowView.findViewById(R.id.systemName);
	    	nameView.setText(item.displayName);
	    	systemNameView.setText(item.system);
			rowView.setOnClickListener(listItemClickListener);
			rowView.setTag(item);
	    	return rowView;
	    }
	  }
	
	public OnClickListener listItemClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			CoreVO core = (CoreVO) v.getTag();
			String appName = core.corePackage;
			if(core.installed){
				
			}else{
				try {
				    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
				} catch (android.content.ActivityNotFoundException anfe) {
				    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
				}
			}
		}
	};
}