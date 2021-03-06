package com.yf.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yf.accountmanager.R;
import com.yf.accountmanager.common.FileManager.ConditionalSearchFileFilter;
import com.yf.accountmanager.common.FileManager.FileComparator;
import com.yf.accountmanager.common.FileManager.IFileFilter;
import com.yf.accountmanager.common.IConstants;
import com.yf.accountmanager.ui.CustomizedDialog;
import com.yf.accountmanager.util.CommonUtils;
import com.yf.accountmanager.util.FileUtils;
import com.yf.accountmanager.util.SharePrefUtil;
import com.yf.accountmanager.util.SystemUtil;
import com.yf.filesystem.FileOperationService.ShareViewHolder;

public class FileSearcherAdapter extends BaseAdapter implements
		Runnable{

	private Context context;

	private int resId = R.layout.item4gv_shareitem;

	public List<File> fileList, selectedList, resultList;

	public List<File> searchScope;

	public boolean multiselectorMode = false;

	public FileComparator comparator;

	public IFileFilter iFilter;

	public FileSystemAdapter fileSystemAdapter;

	public boolean stop;
	
	public int maxResultCount;

	// ///////////////////////////////////////////////
	public View dmc;

	public ImageButton sort, screen;

	public Button positiveButton;

	public TextView dropDown;

	public ProgressBar pb;

	// //////////////////////////////////////////////////

	public FileSearcherAdapter(Context context,
			FileSystemAdapter fileSystemAdapter, View dmc, ImageButton sort,
			ImageButton screen, Button positiveButton, TextView dropDown,
			ProgressBar progressBar) {
		super();
		this.context = context;
		this.dmc = dmc;

		maxResultCount=SharePrefUtil.getMaxFileSystemSearchResultCount();
		
		this.sort = sort;
		this.screen = screen;
		this.positiveButton = positiveButton;
		this.dropDown = dropDown;
		this.pb = progressBar;

		comparator = new FileComparator();
		iFilter = new IFileFilter();
		resetOrderType();
		
		fileList = new ArrayList<File>();
		selectedList = new ArrayList<File>();
		resultList = new ArrayList<File>();
		searchScope = new ArrayList<File>();
		this.fileSystemAdapter = fileSystemAdapter;

		searchOver();
		normalMode();
	}
	
	public boolean setMaxResultCount(int count){
		if(maxResultCount==count) return true;
		if(SharePrefUtil.saveMaxFileSystemSearchResultCount(count)){
			maxResultCount=count;
			return true;
		}
		return false;
	}
	
	public boolean isStop(){
		return stop;
	}

	private void searchOver() {
		stop = true;
		pb.setVisibility(View.GONE);
		sort.setEnabled(true);
		screen.setEnabled(true);
		dropDown.setEnabled(true);
		positiveButton.setText(context.getString(R.string.startSearch));
		positiveButton.setActivated(false);

	}

	private void searchBegin() {
		stop = false;
		resetOrderType();
		pb.setVisibility(View.VISIBLE);
		sort.setEnabled(false);
		screen.setEnabled(false);
		dropDown.setEnabled(false);
		positiveButton.setText(context.getString(R.string.cease));
		positiveButton.setActivated(true);
		fileList.clear();
		resultList.clear();
		refresh(true);
	}
	private void resetOrderType(){
		comparator.setType(-1);
		iFilter.setType(0);
	}
	

	public void setScope(List<File> files) {
		this.searchScope.clear();
		for (int i = 0; i < files.size(); i++) {
			this.searchScope.add(files.get(i));
		}
	}

	public void setScope(File file) {
		this.searchScope.clear();
		this.searchScope.add(file);
	}

	// changeMode
	public boolean toggleMode() {
		if (multiselectorMode) {
			normalMode();
		} else {
			multiselectorMode();
		}
		notifyDataSetChanged();
		return multiselectorMode;
	}

	public void multiselectorMode() {
		selectedList.clear();
		multiselectorMode = true;
		dmc.setVisibility(View.VISIBLE);
	}

	public void normalMode() {
		selectedList.clear();
		multiselectorMode = false;
		dmc.setVisibility(View.GONE);
	}

	public void selectedAll() {
		multiselectorMode();
		for (int i = 0; i < fileList.size(); i++) {
			selectedList.add(fileList.get(i));
		}
		notifyDataSetChanged();
	}

	public void updateSelectedList() {
		for (int i = 0; i < selectedList.size(); i++) {
			File file = selectedList.get(i);
			if (!file.exists()) {
				selectedList.remove(file);
				i--;
			}
		}
	}

	// copy
	public boolean addToCopyList(File file, View convertView) {
		return fileSystemAdapter.addToCopyList(file, convertView);
	}

	// move
	public boolean addToMoveList(File file, View convertView) {
		return fileSystemAdapter.addToMoveList(file, convertView);
	}

	public void addSelectedToCopyList() {
		updateSelectedList();
		if (selectedList.isEmpty()) {
			CommonUtils.toast("无选中文件");
			return;
		}
		int count = 0;
		for (int i = 0; i < selectedList.size(); i++) {
			File file = selectedList.get(i);
			if (file.canRead()) {
				if (!fileSystemAdapter.copyList.contains(file)) {
					if (fileSystemAdapter.moveList.contains(file)) {
						fileSystemAdapter.moveList.remove(file);
					}
					fileSystemAdapter.copyList.add(file);
				}
				count++;
			}
		}
		if (count == selectedList.size()) {
			CommonUtils.toast("复制 " + count + " 个文件");
		} else {
			CommonUtils.toast("复制 " + count + " 个文件, 忽略 "
					+ (selectedList.size() - count) + " 个不可读取文件");
		}
		refresh(true);
	}

	public void addSelectedToMoveList() {
		updateSelectedList();
		if (selectedList.isEmpty()) {
			CommonUtils.toast("无选中文件");
			return;
		}
		int count = 0;
		for (int i = 0; i < selectedList.size(); i++) {
			File file = selectedList.get(i);
			if (file.canRead() && file.getParentFile().canWrite()) {
				if (!fileSystemAdapter.moveList.contains(file)) {
					if (fileSystemAdapter.copyList.contains(file)) {
						fileSystemAdapter.copyList.remove(file);
					}
					fileSystemAdapter.moveList.add(file);
				}
				count++;
			}

		}
		if (count == selectedList.size()) {
			CommonUtils.toast("剪切 " + count + " 个文件");
		} else {
			CommonUtils.toast("剪切 " + count + " 个文件, 忽略 "
					+ (selectedList.size() - count) + " 个不可读取或无法移动的文件");
		}
		refresh(true);
	}

	// sortBYType
	public boolean sort(int type) {
		if (comparator.setType(type) && !fileList.isEmpty()) {
			Collections.sort(fileList,comparator);
			refresh(false);
			return true;
		}
		return false;
	}

	// screenByType
	public boolean screen(int type) {
		if (iFilter.setType(type)) {
			screenNsort();
			return true;
		}
		return false;
	}

	@Override
	public int getCount() {
		if (fileList == null)
			return 0;
		return fileList.size();
	}

	@Override
	public Object getItem(int position) {
		if (fileList == null)
			return null;
		return fileList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ShareViewHolder holder = null;
		if (convertView == null) {
			holder = new ShareViewHolder(convertView = LayoutInflater.from(
					context).inflate(resId, parent, false));
		} else
			holder = (ShareViewHolder) convertView.getTag();

		File file = fileList.get(position);

		if (fileSystemAdapter.moveList.contains(file)) {
			convertView.setEnabled(false);
		} else {
			convertView.setEnabled(true);
		}

		if (fileSystemAdapter.copyList.contains(file))
			holder.label.setEnabled(false);
		else
			holder.label.setEnabled(true);

		if (selectedList.contains(file)) {
			convertView.setActivated(true);
		} else
			convertView.setActivated(false);
		FileUtils.setFileIconByType(file, holder.icon, holder.label);
		return convertView;
	}
	
	
	public void refresh(boolean normalMode){
		if(normalMode)
			normalMode();
		notifyDataSetChanged();
	}

	// screenNsort
	public void screenNsort() {
		fileList.clear();
		if (resultList != null) {
			for (int i = 0; i < resultList.size(); i++) {
				File f = resultList.get(i);
				if (iFilter.accept(f))
					fileList.add(f);
			}
			
			Collections.sort(fileList, comparator);
		}
		if (dmc != null) {
			normalMode();
		}
		notifyDataSetInvalidated();
	}
	
	

	public int   onItemClicked( View self, int position) {
		File file = fileList.get(position);
		if (multiselectorMode) {
			if (selectedList.contains(file)) {
				selectedList.remove(file);
				self.setActivated(false);
			} else {
				selectedList.add(file);
				self.setActivated(true);
			}
		} else {
			if (file.isDirectory()) {
				fileSystemAdapter.changeDir(file);
				return -2;
			} else if (FileUtils.isReadableFile(file)) {
				int type =FileUtils.getFileTYpe(file);
				if(type>=0&&type<5){
//					SystemUtil.openAs(file, type, context);
					return type;
				}else
					SystemUtil.forwardToFileContentViewer(file, context);
			} else {
				CommonUtils.toast("无法读取文件");
			}
		}
		return -1;

	}

	public void selectFile(File file, View self) {
		if (multiselectorMode) {
			if (selectedList.contains(file)) {
				selectedList.remove(file);
				self.setActivated(false);
			} else {
				selectedList.add(file);
				self.setActivated(true);
			}
		}
	}

	// deleteFileAsyn
	public void deleteFileAsynchronized(final File file, Context context) {
		if (!file.getParentFile().canWrite()) {
			if (file.isDirectory())
				CommonUtils.toast("无法删除该文件夹");
			else
				CommonUtils.toast("无法删除该文件");
			return;
		}
		final ProgressDialog pd = CustomizedDialog.createProgressDialogNshow(
				null, "删除文件。。。", false, context);
		IConstants.THREAD_POOL.submit(new Runnable() {
			public void run() {
				final boolean b = FileUtils.deleteFileRecursively(file, false);
				IConstants.MAIN_HANDLER.post(new Runnable() {
					public void run() {
						if (b) {
							CommonUtils.toast(IConstants.DELETE_SUCCESS);
							refresh(false);
						} else
							CommonUtils.toast(IConstants.DELETE_FAIL);
					}
				});
				pd.dismiss();
			}
		});
	}

	public void deleteSelectedListAsynchronized(Context context) {
		if (selectedList.isEmpty())
			return;
		int size = selectedList.size();
		for (int i = size - 1; i >= 0; i--) {
			File file = selectedList.get(i);
			if (!file.getParentFile().canWrite()) {
				selectedList.remove(i);
			}
		}
		int delta = size - selectedList.size();
		if(delta==size){
			CommonUtils.toast("无法删除选中文件[夹]");
			refresh(false);
			return;
		}

		final ProgressDialog pd = CustomizedDialog
				.createProgressDialogNshow(
						null,
						delta == 0 ? context
								.getString(R.string.multiDeleteNotice)
								: context.getString(
										R.string.multiDeleteNotice2, delta),
						false, context);
		IConstants.THREAD_POOL.submit(new Runnable() {
			public void run() {
				boolean b = true;
				for (int i = 0; i < selectedList.size(); i++) {
					b &= FileUtils.deleteFileRecursively(selectedList.get(i),
							false);
				}
				final boolean boo = b;
				IConstants.MAIN_HANDLER.post(new Runnable() {
					public void run() {
						if (boo) {
							CommonUtils.toast(IConstants.DELETE_SUCCESS);
						} else
							CommonUtils.toast(IConstants.DELETE_FAIL);
						refresh(true);
					}
				});
				pd.dismiss();
			}
		});
	}

	// renameFile
	public int renameFile(File file, String newname) {
		int result = FileOperationService.renameFile(file, newname);
		if (result == 1)
			refresh(false);
		return result;
	}

	public boolean inflateFile(File tar, File dest, Context context,
			BaseAdapter adapter) {
		return FileOperationService.inflateFile(tar, dest, context, adapter);
	}

	public void startSearch(String key, ConditionalSearchFileFilter filter) {
		searchBegin();
		IConstants.THREAD_POOL.submit(new FileSeacher(key, filter));
	}

	public void stopSearch() {
		stop=true;
		IConstants.MAIN_HANDLER.post(new Runnable(){
			public void run(){
				searchOver();
			}
		});
		
	}
	
	private boolean  addToResult(File file){
		resultList.add(file);
		fileList.add(file);
		IConstants.MAIN_HANDLER.post(this);
		return fileList.size()<maxResultCount;
	}

	
	
	
	public void run(){
		notifyDataSetChanged();
	}
	
	
	
	
	/***
	 * 
	 * @author Administrator
	 *
	 */
	class FileSeacher implements Runnable {

		ConditionalSearchFileFilter filter;
		String key;

		FileSeacher(String key, ConditionalSearchFileFilter filter) {
			this.key = key;
			this.filter = filter;
		}

		@Override
		public void run() {
//			long milli=System.currentTimeMillis();
			if (TextUtils.isEmpty(key)){
				stopSearch();
				return;
			}
			if (filter == null)
				for (int i = 0; !stop && i < searchScope.size(); i++) {
					File file = searchScope.get(i);
					match(file);
				}
			else{
				filter.key=this.key;
				for (int i = 0; !stop && i < searchScope.size(); i++) {
					File file = searchScope.get(i);
					filterMatch(file);
				}
			}
			if(!stop)
				stopSearch();
//			Log.e("aa",(System.currentTimeMillis()-milli)/100+"   @FileSearcherAdapter");
//			System.out.println(fileList.size()+ "  final   @FileSearcherAdapter");
			
		}

		public void match(File file){
			if(ConditionalSearchFileFilter.accept(file, key)&&!addToResult(file)){
				stopSearch();
				return;
			}
			if(file.isDirectory()){
				File[] files = file.listFiles();
				for(int i=0;!stop&&files!=null&&i<files.length;i++){
					match(files[i]);
				}
			}
		}

		public void filterMatch(File file) {
			if(filter.accept(file)&&!addToResult(file)){
				stopSearch();
				return;
			}
			if(file.isDirectory()){
				File[] files = file.listFiles();
			
				for(int i=0;!stop&&files!=null&&i<files.length;i++){
					filterMatch(files[i]);
				}
			}
		}

	}

}
