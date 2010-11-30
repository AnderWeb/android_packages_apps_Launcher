package com.android.launcher;


public interface Drawer {

	public int getVisibility();

	public boolean isOpaque();

	public boolean hasFocus();

	public boolean requestFocus();

	public void setTextFilterEnabled(boolean textFilterEnabled);
	public void clearTextFilter();


	public void setDragger(DragController dragger);
	public void setLauncher(Launcher launcher);
	public void updateAppGrp();
	public void setNumColumns(int numColumns);
	public void setNumRows(int numRows);
	public void setPageHorizontalMargin(int margin);
	public void setAdapter(ApplicationsAdapter adapter);
	public void setAnimationSpeed(int speed);
	public void open(boolean animate);
	public void close(boolean animate);
	public void setPadding(int left, int top, int right, int bottom);
}
