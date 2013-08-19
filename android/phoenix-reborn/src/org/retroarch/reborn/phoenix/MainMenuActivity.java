package org.retroarch.reborn.phoenix;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.retroarch.reborn.phoenix.data.CoreVO;
import org.retroarch.reborn.phoenix.ui.CoreAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;

public class MainMenuActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);
		try{
			addCores();
		}catch(Exception e){
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);

		return true;
	}

	private void addCores() throws ParserConfigurationException, IOException, SAXException{
		createCoresList.execute();
	}
	
	private void createCoresListComplete(ArrayList<CoreVO> allCores){
		CoreAdapter adapter = new CoreAdapter(this, allCores);
		ListView list = (ListView) findViewById(R.id.coresList);
		list.setAdapter(adapter);
	}
	
	private boolean checkIfModuleIsInstalled(String module){
		PackageManager pm = getPackageManager();
		try {
			pm.getPackageInfo(module, PackageManager.GET_ACTIVITIES);
			return true;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private AsyncTask<Void, Void, Void> createCoresList = new AsyncTask<Void, Void, Void>(){
		protected ArrayList<CoreVO> allCores;
		
		@Override
		protected Void doInBackground(Void... params) {
			Document doc = getDocument();
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("core");
			
			ArrayList<CoreVO> installedCores = new ArrayList<CoreVO>();
			installedCores.add(new CoreVO(CoreVO.TYPE_TITLE, getString(R.string.installed_cores)));
			ArrayList<CoreVO> notInstalledCores = new ArrayList<CoreVO>();
			notInstalledCores.add(new CoreVO(CoreVO.TYPE_TITLE, getString(R.string.available_cores)));
			
			for (int i = 0; i < nodeList.getLength(); i++) 
	        {
	            Node node = nodeList.item(i);
	            Element element = (Element) node;
	            String id = element.getElementsByTagName("id").item(0).getTextContent();
	            String name = element.getElementsByTagName("name").item(0).getTextContent();
	            String coreSystem = element.getElementsByTagName("system").item(0).getTextContent();
	            String corePackage = element.getElementsByTagName("package").item(0).getTextContent();
	            boolean installed = checkIfModuleIsInstalled(corePackage);
	            if(installed){
	            	installedCores.add(new CoreVO(CoreVO.TYPE_ITEM, id, name, coreSystem, corePackage, installed));
	            }else{
	            	notInstalledCores.add(new CoreVO(CoreVO.TYPE_ITEM, id, name, coreSystem, corePackage, installed));
	            }
	        }
			
			allCores = new ArrayList<CoreVO>();
			allCores.addAll(installedCores);
			allCores.addAll(notInstalledCores);
			
			return null;
		}
		
		protected void onPostExecute(Void result) {
			createCoresListComplete(allCores);
		}
		
		protected Document getDocument(){
			InputSource is = new InputSource(getResources().openRawResource(R.raw.cores));
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
				Document doc = db.parse(new InputSource(is.getByteStream()));
				return doc;
			} catch (Exception e) {
				return null;
			}
		}
	};
}

