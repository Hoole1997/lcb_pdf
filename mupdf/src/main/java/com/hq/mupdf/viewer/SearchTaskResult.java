package com.hq.mupdf.viewer;

import com.artifex.mupdf.fitz.Quad;

public class SearchTaskResult {
	public final String txt;
	public final int pageNumber;
	public final Quad searchBoxes[][];
	static private SearchTaskResult singleton;

	SearchTaskResult(String _txt, int _pageNumber, Quad _searchBoxes[][]) {
		txt = _txt;
		pageNumber = _pageNumber;
		searchBoxes = _searchBoxes;
	}

	static public SearchTaskResult get() {
		return singleton;
	}

	static public void set(SearchTaskResult r) {
		singleton = r;
	}
	
	/**
	 * 创建搜索结果的公共工厂方法
	 */
	static public SearchTaskResult create(String txt, int pageNumber, Quad searchBoxes[][]) {
		return new SearchTaskResult(txt, pageNumber, searchBoxes);
	}
}
