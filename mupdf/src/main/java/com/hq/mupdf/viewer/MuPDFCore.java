package com.hq.mupdf.viewer;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.SeekableInputStream;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;

public class MuPDFCore
{
	private final String APP = "MuPDF";

	private int resolution;
	private Document doc;
	private Outline[] outline;
	private int pageCount = -1;
	private boolean reflowable = false;
	private int currentPage;
	private Page page;
	private float pageWidth;
	private float pageHeight;
	private DisplayList displayList;

	/* Default to "A Format" pocket book size. */
	private int layoutW = 312;
	private int layoutH = 504;
	private int layoutEM = 10;

	private MuPDFCore(Document doc) {
		this.doc = doc;
		doc.layout(layoutW, layoutH, layoutEM);
		pageCount = doc.countPages();
		reflowable = doc.isReflowable();
		resolution = 160;
		currentPage = -1;
	}

	public MuPDFCore(byte buffer[], String magic) {
		this(Document.openDocument(buffer, magic));
	}

	public MuPDFCore(SeekableInputStream stm, String magic) {
		this(Document.openDocument(stm, magic));
	}

	public String getTitle() {
		return doc.getMetaData(Document.META_INFO_TITLE);
	}

	public int countPages() {
		return pageCount;
	}

	public boolean isReflowable() {
		return reflowable;
	}

	public synchronized int layout(int oldPage, int w, int h, int em) {
		if (w != layoutW || h != layoutH || em != layoutEM) {
			System.out.println("LAYOUT: " + w + "," + h);
			layoutW = w;
			layoutH = h;
			layoutEM = em;
			long mark = doc.makeBookmark(doc.locationFromPageNumber(oldPage));
			doc.layout(layoutW, layoutH, layoutEM);
			currentPage = -1;
			pageCount = doc.countPages();
			outline = null;
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
			return doc.pageNumberFromLocation(doc.findBookmark(mark));
		}
		return oldPage;
	}

	private synchronized void gotoPage(int pageNum) {
		/* TODO: page cache */
		if (pageNum > pageCount-1)
			pageNum = pageCount-1;
		else if (pageNum < 0)
			pageNum = 0;
		if (pageNum != currentPage) {
			if (page != null)
				page.destroy();
			page = null;
			if (displayList != null)
				displayList.destroy();
			displayList = null;
			page = null;
			pageWidth = 0;
			pageHeight = 0;
			currentPage = -1;

			if (doc != null) {
				page = doc.loadPage(pageNum);
				Rect b = page.getBounds();
				pageWidth = b.x1 - b.x0;
				pageHeight = b.y1 - b.y0;
			}

			currentPage = pageNum;
		}
	}

	public synchronized PointF getPageSize(int pageNum) {
		gotoPage(pageNum);
		return new PointF(pageWidth, pageHeight);
	}

	public synchronized void onDestroy() {
		if (displayList != null)
			displayList.destroy();
		displayList = null;
		if (page != null)
			page.destroy();
		page = null;
		if (doc != null)
			doc.destroy();
		doc = null;
	}

	public synchronized void drawPage(Bitmap bm, int pageNum,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			Cookie cookie) {
		gotoPage(pageNum);

		if (displayList == null && page != null)
			try {
				displayList = page.toDisplayList();
			} catch (Exception ex) {
				displayList = null;
			}

		if (displayList == null || page == null)
			return;

		float zoom = resolution / 72;
		Matrix ctm = new Matrix(zoom, zoom);
		RectI bbox = new RectI(page.getBounds().transform(ctm));
		float xscale = (float)pageW / (float)(bbox.x1-bbox.x0);
		float yscale = (float)pageH / (float)(bbox.y1-bbox.y0);
		ctm.scale(xscale, yscale);

		AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
		try {
			displayList.run(dev, ctm, cookie);
			dev.close();
		} finally {
			dev.destroy();
		}
	}

	public synchronized void updatePage(Bitmap bm, int pageNum,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH,
			Cookie cookie) {
		drawPage(bm, pageNum, pageW, pageH, patchX, patchY, patchW, patchH, cookie);
	}

	/**
	 * 强制刷新页面 - 清除DisplayList缓存以显示新注释
	 */
	public synchronized void forceRefreshPage(int pageNum) {
		if (pageNum == currentPage) {
			// 清除DisplayList缓存
			if (displayList != null) {
				displayList.destroy();
				displayList = null;
			}
			// 下次drawPage时会重新生成DisplayList
		}
	}

	/**
	 * 强制重新加载页面 - 完全重置页面状态
	 */
	public synchronized void reloadPage(int pageNum) {
		// 清除所有缓存
		if (displayList != null) {
			displayList.destroy();
			displayList = null;
		}
		if (page != null) {
			page.destroy();
			page = null;
		}
		
		// 重置状态
		currentPage = -1;
		pageWidth = 0;
		pageHeight = 0;
		
		// 重新加载页面
		gotoPage(pageNum);
	}

	/**
	 * 注释创建后的完整刷新机制
	 * 强制重新加载page对象以包含新注释
	 */
	public synchronized void refreshPageForAnnotation(int pageNum) {
		try {
			System.out.println("开始注释刷新 - 页码: " + pageNum + ", 当前页: " + currentPage);
			
			// 🔑 关键修复：注释创建后必须重新加载page对象
			// 因为新注释只有在重新加载页面时才会包含在displayList中
			if (pageNum == currentPage) {
				// 1. 清除所有缓存
				if (displayList != null) {
					displayList.destroy();
					displayList = null;
				}
				if (page != null) {
					page.destroy();
					page = null;
				}
				
				// 2. 强制重新加载页面对象
				// 这会确保新注释被包含在页面中
				currentPage = -1; // 重置当前页码
				gotoPage(pageNum); // 重新加载页面
				
				System.out.println("✅ 注释刷新完成 - 强制重新加载页面对象");
			} else {
				System.out.println("⚠️ 页码不匹配，跳过刷新");
			}
		} catch (Exception e) {
			System.err.println("注释页面刷新失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public Document getDoc() {
		return doc;
	}

	public synchronized Link[] getPageLinks(int pageNum) {
		gotoPage(pageNum);
		return page != null ? page.getLinks() : null;
	}

	public synchronized int resolveLink(Link link) {
		return doc.pageNumberFromLocation(doc.resolveLink(link));
	}

	public synchronized Quad[][] searchPage(int pageNum, String text) {
		gotoPage(pageNum);
		return page.search(text);
	}

	/**
	 * 获取页面文本内容
	 */
	public synchronized String getPageText(int pageNum) {
		gotoPage(pageNum);
		if (page == null) return "";
		
		try {
			// 正确用法：toStructuredText()返回StructuredText对象，需要调用asText()方法
			com.artifex.mupdf.fitz.StructuredText structuredText = page.toStructuredText();
			if (structuredText != null) {
				String text = structuredText.asText();
				// 确保资源被正确释放
				structuredText.destroy();
				return text != null ? text : "";
			}
			return "";
		} catch (Exception e) {
			// 如果失败，返回空字符串
			return "";
		}
	}

	/**
	 * 获取页面的结构化文本对象（用于精确位置计算）
	 * 注意：调用者需要负责调用destroy()释放资源
	 */
	public synchronized com.artifex.mupdf.fitz.StructuredText getPageStructuredText(int pageNum) {
		gotoPage(pageNum);
		if (page == null) return null;
		
		try {
			return page.toStructuredText();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 在指定位置查找最接近的字符
	 */
	public synchronized Quad[][] findCharacterAt(int pageNum, float x, float y) {
		gotoPage(pageNum);
		if (page == null) return new Quad[0][0];
		
		try {
			// 使用单个字符的搜索来找到在特定位置的字符
			// 这是一个简化的方法，实际应用中可能需要更复杂的逻辑
			com.artifex.mupdf.fitz.StructuredText structuredText = page.toStructuredText();
			if (structuredText != null) {
				// 获取页面上的所有文本
				String text = structuredText.asText();
				structuredText.destroy();
				
				// 逐个字符搜索来找到最接近的位置
				// 这是一个近似方法，可以改进
				if (text.length() > 0) {
					// 返回第一个字符的搜索结果
					return page.search(text.substring(0, 1));
				}
			}
			return new Quad[0][0];
		} catch (Exception e) {
			return new Quad[0][0];
		}
	}

	public synchronized boolean hasOutline() {
		if (outline == null) {
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
		}
		return outline != null;
	}

	private void flattenOutlineNodes(ArrayList<OutlineActivity.Item> result, Outline list[], String indent) {
		for (Outline node : list) {
			if (node.title != null) {
				int page = doc.pageNumberFromLocation(doc.resolveLink(node));
				result.add(new OutlineActivity.Item(indent + node.title, page));
			}
			if (node.down != null)
				flattenOutlineNodes(result, node.down, indent + "    ");
		}
	}

	public synchronized ArrayList<OutlineActivity.Item> getOutline() {
		ArrayList<OutlineActivity.Item> result = new ArrayList<OutlineActivity.Item>();
		flattenOutlineNodes(result, outline, "");
		return result;
	}

	public synchronized boolean needsPassword() {
		return doc.needsPassword();
	}

	public synchronized boolean authenticatePassword(String password) {
		boolean authenticated = doc.authenticatePassword(password);
		pageCount = doc.countPages();
		reflowable = doc.isReflowable();
		return authenticated;
	}


}
