package com.example.securepicturetransfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Filter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

/***
 * This class is for displaying the contacts in list view
 * @author fa45
 *
 */
public class CustomListViewAdapter extends ArrayAdapter<RowItem> implements SectionIndexer
{
	private HashMap<String, Integer> alphaIndexer;
    private String[] sections;
    
    Context context;
 
    public CustomListViewAdapter(Context context, int resourceId,List<RowItem> items) 
    {
        super(context, resourceId, items);
        this.context = context;
        
        alphaIndexer = new HashMap<String, Integer>();
        for (int i = 0; i < items.size(); i++)
        {
            String s = items.get(i).getName().substring(0, 1).toUpperCase();
            if (!alphaIndexer.containsKey(s))
                alphaIndexer.put(s, i);
        }
        
        Set<String> sectionLetters = alphaIndexer.keySet();
        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
        Collections.sort(sectionList);
        sections = new String[sectionList.size()];
        for (int i = 0; i < sectionList.size(); i++)
            sections[i] = sectionList.get(i); 
    }
 
    /*private view holder class*/
    private class ViewHolder 
    {
        ImageView imageView;
        TextView name;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) 
    {
        ViewHolder holder = null;
        RowItem rowItem = getItem(position);
 
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        
        if (convertView == null) 
        {
            convertView = mInflater.inflate(R.layout.simplerow, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.rowTextView);
            holder.imageView = (ImageView) convertView.findViewById(R.id.img);
            
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder) convertView.getTag();
 
        holder.name.setText(rowItem.getName());
        holder.imageView.setImageResource(rowItem.getImageId());
 
        return convertView;
    }

	public void doFilter(ListView mainListView, String charSeq, List<RowItem> rowItems) 
	{
		List<RowItem> newRow = new ArrayList<RowItem>();
		
		for(int i = 0 ; i < rowItems.size(); i++)
		{
			if (rowItems.get(i).getName().toLowerCase().contains(charSeq.toLowerCase()))
			{
				newRow.add(rowItems.get(i));
			}
		}
		

		 mainListView.setAdapter( new CustomListViewAdapter(context, R.layout.simplerow, newRow) );     
		
	}

	@Override
	public int getPositionForSection(int section) {
		// TODO Auto-generated method stub
		return alphaIndexer.get(sections[section]);
	}

	@Override
	public int getSectionForPosition(int arg0) {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public Object[] getSections() {
		// TODO Auto-generated method stub
		return sections;
	}
    

  
}