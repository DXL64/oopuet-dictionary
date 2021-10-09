package test;

import java.io.IOException;
import java.util.List;

import com.knziha.plod.dictionary.mdict;


public class resultRecorderScattered2 extends  dict_Activity_resultRecoder{
	
	private List<mdict> md;
	
	private int[] firstLookUpTable;
	
	private int size=0;
	
	@Override
	public void invalidate() {
		if(firstLookUpTable.length<md.size())
			firstLookUpTable = new int[md.size()];
		
		int resCount=0;
		for(int i=0;i<md.size();i++){//遍历所有词典
			mdict mdtmp = md.get(i);
			//int baseCount=0;
			//if(i!=0)
			//	baseCount=firstLookUpTable[i-1];
			if(mdtmp.combining_search_tree_4==null) {
			}
			else
    		for(int ti=0;ti<mdtmp.combining_search_tree_4.length;ti++){//遍历搜索结果
    			if(mdtmp.combining_search_tree_4[ti]==null) {
    				continue;
    			}
    			resCount+=mdtmp.combining_search_tree_4[ti].size();
    		}
			firstLookUpTable[i]=resCount;
			
		}
		
		size=resCount;
	}
	
	@Override
	public void invalidate(int idx) {
		if(firstLookUpTable.length<md.size())
			firstLookUpTable = new int[md.size()];

		int resCount=0;
		mdict mdtmp = md.get(idx);
		
		if(mdtmp.combining_search_tree_4==null) {
		}else
		for(int ti=0;ti<mdtmp.combining_search_tree_4.length;ti++){//遍历搜索结果
			if(mdtmp.combining_search_tree_4[ti]==null) {
				continue;
			}
			resCount+=mdtmp.combining_search_tree_4[ti].size();
		}
		
		//firstLookUpTable[idx]=resCount;
		for(int i=0;i<firstLookUpTable.length;i++) {
			if(i<idx)
				firstLookUpTable[i] = 0;
			else
				firstLookUpTable[i] = resCount;
		}
		size=resCount;
	}
	
	resultRecorderScattered2(List<mdict> md_){
		super();
		md=md_;
		firstLookUpTable = new int[md_.size()];
	}
	
	
	@Override
	public String getResAt(int pos) {
		if(size<=0 || pos<0 || pos>size-1)
			return "!!! Error: code 1";
		int Rgn = binary_find_closest(firstLookUpTable,pos+1,md.size());
		if(Rgn<0 || Rgn>md.size()-1)
			return "!!! Error: code 2 Rgn="+Rgn+" size="+md.size();
		mdict mdtmp = md.get(Rgn);
		dictIdx=Rgn;
		if(Rgn!=0)
			pos-=firstLookUpTable[Rgn-1];
		int idxCount = 0;
		for(int ti=0;ti<mdtmp.combining_search_tree_4.length;ti++){
			if(mdtmp.combining_search_tree_4[ti]==null)
				continue;
			int max = mdtmp.combining_search_tree_4[ti].size();
			if(max==0)
				continue;
			if(pos-idxCount<max) {
				return mdtmp.getEntryAt(mdtmp.combining_search_tree_4[ti].get(pos-idxCount));
			}
			idxCount+=max;
		}
		return "!!! Error: code 3 ";
	};
	
	@Override
	public void renderContentAt(int pos){
		getResAt(pos);
		if(size<=0 || pos<0 || pos>size-1)
			return;
		int Rgn = binary_find_closest(firstLookUpTable,pos+1,md.size());
		if(Rgn<0 || Rgn>md.size()-1)
			return;
		mdict mdtmp = md.get(Rgn);
		if(Rgn!=0)
			pos-=firstLookUpTable[Rgn-1];
		int idxCount = 0;
		for(int ti=0;ti<mdtmp.combining_search_tree_4.length;ti++){
			if(mdtmp.combining_search_tree_4[ti]==null)
				continue;
			int max = mdtmp.combining_search_tree_4[ti].size();
			if(max==0)
				continue;
			if(pos-idxCount<max) {
				int recpos = mdtmp.combining_search_tree_4[ti].get(pos-idxCount);
				try {
					CMN.show(mdtmp.getRecordAt(recpos));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			idxCount+=max;
		}
	};
	
	@Override
	public int size(){
		return size;
	};
	
	@Override
	public void shutUp() {
		size=0;
	}

    public static int  binary_find_closest(int[] array,int val,int iLen){
    	int middle = 0;
    	if(iLen==-1||iLen>array.length)
    		iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		//CMN.show(low+"~"+high);
    		counter+=1;
    		System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		cprRes1=array[middle+1]-val;
        	cprRes0=array[middle  ]-val;
        	if(cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		//high=middle;
        		low=middle+1;//here
        	}
    	}
		return low;
    }
	
}
