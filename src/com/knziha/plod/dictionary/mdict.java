/*  Copyright 2018 KnIfER Zenjio-Kang

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
	
	Mdict-Java Query Library
*/

package com.knziha.plod.dictionary;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;
//import org.jvcompress.lzo.MiniLZO;
//import org.jvcompress.util.MInt;
import org.apache.commons.lang.StringEscapeUtils;


import com.knziha.rbtree.RBTree_additive;


/**
 * Mdict Java Library
 * FEATURES:
 * 1. Basic parse and query functions.
 * 2. Mdicts conjunction search.
 * 3. Multi-threaded search in all context text.
 * 4. Multi-threaded fuzzy search in all key entries.
 * @author KnIfER
 * @date 2017/12/30
 */

public class mdict extends mdBase{
	public mdict(){};//don't call this.
	
	public final static Pattern replaceReg = Pattern.compile(" |:|\\.|,|-|\'|\\(|\\)|#|<|>|!|\\n");
	public final static Pattern markerReg = Pattern.compile("`([\\w\\W]{1,3}?)`");// for `1` `2`...
	public final static Pattern imageReg = Pattern.compile("\\.jpg|\\.bmp|\\.eps|\\.gif|\\.png|\\.tif|\\.tiff|\\.svg|\\.jpe|\\.jpeg|\\.ico|\\.tga|\\.pic$", Pattern.CASE_INSENSITIVE);
	public final static Pattern htmlReg = Pattern.compile("\\.html$", Pattern.CASE_INSENSITIVE);
	public final static Pattern mobiReg = Pattern.compile("\\.mobi|\\.azw|\\.azw3$", Pattern.CASE_INSENSITIVE);
	public final static Pattern soundReg = Pattern.compile("\\.mp3|\\.ogg|\\.wav|\\.spx$", Pattern.CASE_INSENSITIVE);
	public final static Pattern videoReg = Pattern.compile("\\.mp4|\\.avi$", Pattern.CASE_INSENSITIVE);
    private final static String linkRenderStr = "@@@LINK=";
    
    protected mdictRes mdd;
    
    public String _Dictionary_fName;
    public String _Dictionary_Name;
    public String _Dictionary_fSuffix;
    
	//构造
	public mdict(String fn) throws IOException {
		super(fn);
        _Dictionary_fName = f.getName();
    	int tmpIdx = _Dictionary_fName.lastIndexOf(".");
    	if(tmpIdx!=-1) {
	    	_Dictionary_fSuffix = _Dictionary_fName.substring(tmpIdx+1);
	    	_Dictionary_fName = _Dictionary_fName.substring(0, tmpIdx);
    	}
        String fnTMP = f.getName();
        File f2 = new File(f.getParentFile().getAbsolutePath()+"/"+fnTMP.substring(0,fnTMP.lastIndexOf("."))+".mdd");
    	if(f2.exists()){
    		mdd=new mdictRes(f2.getAbsolutePath());
    	}
    	if(_header_tag.containsKey("Title"))
			_Dictionary_Name=_header_tag.get("Title");
    	calcFuzzySpace();
	}
	
	public int reduce2(byte[] phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return compareByteArray(phrase, _key_block_info_list[start + len - 1].tailerKeyText)>0
                    ? reduce2(phrase,start+len,end)
                    : reduce2(phrase,start,start+len);
        } else {
          return start;
        }
    }
	public int reduce(String phrase,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return phrase.compareTo(new String(_key_block_info_list[start + len - 1].tailerKeyText,_charset))>0
                    ? reduce(phrase,start+len,end)
                    : reduce(phrase,start,start+len);
        } else {
          return start;
        }
    }
	public int lookUp(String keyword) {
		return lookUp(keyword,false);
	}
	public int lookUp(String keyword,boolean isSrict)
    {
    	if(_key_block_info_list==null) read_key_block_info();
    	keyword = processText(keyword);
    	byte[] kAB = keyword.getBytes(_charset);
    	
    	int blockId = -1;
    	
    	if(_encoding.startsWith("GB")) {
    		int boudaryCheck = compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB);
    		if(boudaryCheck<0)
    			return -1;
    		if(boudaryCheck==0) blockId = (int)_num_key_blocks-1;
    		boudaryCheck = compareByteArray(_key_block_info_list[0].headerKeyText,kAB);
    		if(boudaryCheck>0)
    			return -1;
    		if(boudaryCheck==0) return 0;
    	}else {
    		int boudaryCheck = new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).compareTo(keyword);
    		if(boudaryCheck<0)
    			return -1;
    		if(boudaryCheck==0) blockId = (int)_num_key_blocks-1;
    		boudaryCheck = new String(_key_block_info_list[0].headerKeyText,_charset).compareTo(keyword);
    		if(boudaryCheck>0)
    			return -1;
    		if(boudaryCheck==0) return 0;
    	}
    	if(blockId==-1)
    		blockId = _encoding.startsWith("GB")?reduce2(keyword.getBytes(_charset),0,_key_block_info_list.length):reduce(keyword,0,_key_block_info_list.length);
        if(blockId==-1) return blockId;
        //show("blockId:"+blockId);
        //while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0) blockId--;
        //CMN.show("finally blockId is:"+blockId+":"+_key_block_info_list.length);
    	
        
        key_info_struct infoI = _key_block_info_list[blockId];

        cached_key_block infoI_cache = prepareItemByKeyInfo(infoI,blockId,null);
        
        int res;
        if(_encoding.startsWith("GB"))
        	//res = binary_find_closest2(infoI_cache.keys,keyword);//keyword
    		res = reduce_keys2(infoI_cache.keys,kAB,0,infoI_cache.keys.length);
        else
        	//res = binary_find_closest(infoI_cache.keys,keyword);//keyword
    		res = reduce_keys(infoI_cache.keys,keyword,0,infoI_cache.keys.length);
        	
        if (res==-1){
        	System.out.println("search failed!"+keyword);
        	return -1;
        }
        else{
        	if(isSrict)
        		if(!processText(new String(infoI_cache.keys[res],_charset)).equals(keyword))
            		return -1;
        	//String KeyText= infoI_cache.keys[res];
        	//for(String ki:infoI.keys) CMN.show(ki);
        	//show("match key "+KeyText+" at "+res);
        	return (int) (infoI.num_entries_accumulator+res);
        }   
    }

	public int reduce_keys(byte[][] keys,String val,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
          return val.compareTo(processText(new String(keys[start + len - 1],_charset)))>0
                    ? reduce_keys(keys,val,start+len,end)
                    : reduce_keys(keys,val,start,start+len);
        } else {
          return start;
        }
    }
	public int reduce_keys2(byte[][] keys,byte[] val,int start,int end) {//via mdict-js
        int len = end-start;
        if (len > 1) {
          len = len >> 1;
		  //CMN.show(start+"::"+end+"   "+new String(keys[start],_charset)+"::"+new String(keys[end],_charset));
          return compareByteArray(val, processText(new String(keys[start + len - 1],_charset)).getBytes(_charset))>0
                    ? reduce_keys2(keys,val,start+len,end)
                    : reduce_keys2(keys,val,start,start+len);
        } else {
          return start;
        }
    }

    
    public String getRecordsAt(int... positions) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	int c=0;
    	for(int i:positions) {
    		String tmp = getRecordAt(i);
    		if(tmp.startsWith(linkRenderStr)) {
    			//CMN.show(tmp.replace("\n", "1"));
    			String key = tmp.substring(linkRenderStr.length());
    			int offset = offsetByTailing(key);
    			key = key.trim();
    			//CMN.show(offset+"");
    			int idx = lookUp(key);
    			if(idx!=-1) {
    				if(offset>0) {
    					if(key.equals(getEntryAt(idx+offset)))
    						idx+=offset;
    				}
    				tmp=getRecordAt(idx);
    			}
    		}
    		sb.append(tmp);//.trim()
    		if(c!=positions.length-1)
        		sb.append("<HR>");
    		c++;
    	}
    	return processStyleSheet(sb.toString());
    }
    
    public static int offsetByTailing(String token) {
    	//calculating relative offset represented by number of tailing '\n'.
    	//entrys: abc abc acc TO: abc abc\n acc
		if(token.endsWith("\n")) {
			int first=token.length()-1;
			while(first-1>0 && token.charAt(first-1)=='\n') {
				first--;
			}
			return token.length()-first;
		}
		return 0;
	}

	public String getRecordAt(int position) throws IOException {
    	if(record_block_==null)
    		decode_record_block_header();
    	if(position<0||position>=_num_entries) return null;
        int blockId = accumulation_blockId_tree.xxing(new myCpr<Integer,Integer>(position,1)).getKey().value;
        key_info_struct infoI = _key_block_info_list[blockId];
        
        //准备
        prepareItemByKeyInfo(infoI,blockId,null);
        //String[] key_list = infoI_cache_.keys;
        
        //decode record block
        // actual record block data
        int i = (int) (position-infoI.num_entries_accumulator);
        Integer Rinfo_id = reduce(infoI_cache_.key_offsets[i],0,_record_info_struct_list.length);//accumulation_RecordB_tree.xxing(new mdictRes.myCpr(,1)).getKey().value;//null 过 key前
        record_info_struct RinfoI = _record_info_struct_list[Rinfo_id];
        
        byte[] record_block = prepareRecordBlock(RinfoI,Rinfo_id);
        
            
        // split record block according to the offset info from key block
        //String key_text = key_list[i];
        long record_start = infoI_cache_.key_offsets[i]-RinfoI.decompressed_size_accumulator;
        long record_end;
        if (i < infoI.num_entries-1){
        	record_end = infoI_cache_.key_offsets[i+1]-RinfoI.decompressed_size_accumulator; 	
        }//TODO construct a margin checker
        else{
        	if(blockId+1<_key_block_info_list.length) {
        		prepareItemByKeyInfo(null,blockId+1,null);//没办法只好重新准备一个咯
        		//难道还能根据text末尾的0a 0d 00来分？不大好吧、
            	record_end = infoI_cache_.key_offsets[0]-RinfoI.decompressed_size_accumulator;
        	}else
        		record_end = rec_decompressed_size;
        	//CMN.show(record_block.length+":"+compressed_size+":"+decompressed_size);
        }
        //CMN.show(record_start+"!"+record_end);
        //byte[] record = new byte[(int) (record_end-record_start)]; 
        //CMN.show(record.length+":"+record_block.length+":"+(record_start));
        //System.arraycopy(record_block, (int) (record_start), record, 0, record.length);
        // convert to utf-8
        String record_str = new String(record_block,(int) (record_start),(int) (record_end-record_start),_charset);
        // substitute styles
        //if self._substyle and self._stylesheet:
        //    record = self._substitute_stylesheet(record);
		StringBuilder LoPageBuilder = new StringBuilder();
		LoPageBuilder.append(record_str.toString());
		String key = getEntryAt(position);
		if(imageReg.matcher(key).find()){
			System.out.println("IMAGE");
			LoPageBuilder.append("<img style='width:100%; height:auto;' src=\"").append(key).append("\"></img>");
		}
		else if(soundReg.matcher(key).find()){
			System.out.println("SOUND");
			LoPageBuilder.append("<h2>").append(key).append("</h2>");
			LoPageBuilder.append("<audio controls='controls' autoplay='autoplay' src=\"").append(key).append("\"></audio>");
			LoPageBuilder.append("<h2 style='top:56%'>").append(key).append("</h2>");
		}
		else if(videoReg.matcher(key).find()){
			System.out.println("VIDEO");
			LoPageBuilder.append("<video width='320' height='240' controls=\"controls\" src=\"").append(key).append("\"></video>");
		}

        return	LoPageBuilder.toString();
    }
  



    
    long[] keyBlocksHeaderTextKeyID;
	public static long stst;
    //int counter=0;
    public void fetch_keyBlocksHeaderTextKeyID(){
    	int blockId = 0;
    	keyBlocksHeaderTextKeyID = new long[(int)_num_key_blocks];
        byte[] key_block = new byte[(int) maxDecomKeyBlockSize];
    	for(key_info_struct infoI:_key_block_info_list){
    		try {
	            long start = infoI.key_block_compressed_size_accumulator;
	            long compressedSize;
	            if(blockId==_key_block_info_list.length-1)
	                compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
	            else
	                compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
	            
				DataInputStream data_in = getStreamAt(_key_block_offset+start);
				
				byte[]  _key_block_compressed = new byte[(int) compressedSize];
				data_in.read(_key_block_compressed, 0,(int) compressedSize);
				data_in.close();
				
	            if(compareByteArrayIsPara(_zero4, _key_block_compressed)){
	                //无需解压
	                System.arraycopy(_key_block_compressed, 8, key_block, 0,(int) (compressedSize-8));
	            }else if(compareByteArrayIsPara(_1zero3, _key_block_compressed))
	            {
	            	//MInt len = new MInt((int) infoI.key_block_decompressed_size);
	                //byte[] arraytmp = new byte[(int) compressedSize];
	                //CMN.show(key_block.length+":"+infoI.key_block_decompressed_size);
	                //System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
	            	//MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
	                new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block, 0,new lzo_uintp());
	            }
	            else if(compareByteArrayIsPara(_2zero3, _key_block_compressed)){
	                //key_block = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
	            	Inflater inf = new Inflater();
	                inf.setInput(_key_block_compressed, 8 ,(int)(compressedSize-8));
	                try {
						int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
					} catch (DataFormatException e) {e.printStackTrace();}
	            }
	            //!!spliting curr Key block
	            
	            	if(_version<2)
	            		keyBlocksHeaderTextKeyID[blockId] = BU.toInt(key_block, 0);
	            	else
	            		keyBlocksHeaderTextKeyID[blockId] = BU.toLong(key_block, 0);
	
	                blockId++;

			} catch (IOException e) {
				
				e.printStackTrace();
			}
        }
    }
     
    
    volatile int thread_number_count = 1;
    int split_recs_thread_number;
    public void flowerFindAllContents(String key,int selfAtIdx,int theta) throws IOException, DataFormatException{
    	final byte[][] keys = new byte[][] {key.getBytes(_charset),key.toUpperCase().getBytes(_charset),(key.substring(0,1).toUpperCase()+key.substring(1)).getBytes(_charset)};

    	key = key.toLowerCase();
		String upperKey = key.toUpperCase();
    	final byte[][][] matcher = new byte[upperKey.equals(key)?1:2][][];
		matcher[0] = flowerSanLieZhi(key);
		if(matcher.length==2)
		matcher[1] = flowerSanLieZhi(upperKey);
		
 	    if(_key_block_info_list==null) read_key_block_info();

    	if(record_block_==null)
    		decode_record_block_header();
    	
        fetch_keyBlocksHeaderTextKeyID();
        
        
        split_recs_thread_number = _num_record_blocks<6?1:(int) (_num_record_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
        split_recs_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
        final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);
	     
	     
        final int step = (int) (_num_record_blocks/split_recs_thread_number);
    	final int yuShu=(int) (_num_record_blocks%split_recs_thread_number);
    	
		if(combining_search_tree_4==null)
			combining_search_tree_4 = new ArrayList[split_recs_thread_number];
    	
		poolEUSize = dirtykeyCounter =0;
		
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(thread_number);
        for(int ti=0; ti<split_recs_thread_number; ti++){//分  thread_number 股线程运行
        	if(searchCancled) break;
	    	final int it = ti;
	    	if(split_recs_thread_number>thread_number) while (poolEUSize>=thread_number) {
				  try {
					Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}  
				} 

            if(combining_search_tree_4[it]==null)
            	combining_search_tree_4[it] = new ArrayList<Integer>();
            
        	if(split_recs_thread_number>thread_number) countDelta(1);
        	
	        fixedThreadPool.execute(
	        new Runnable(){@Override public void run() 
	        {
	        	if(searchCancled) { poolEUSize=0; return; }
	            final byte[] record_block_compressed = new byte[(int) maxComRecSize];//!!!避免反复申请内存
	            final byte[] record_block_ = new byte[(int) maxDecompressedSize];//!!!避免反复申请内存
	            try 
	            {
		            FileInputStream data_in = new FileInputStream(f);
		            data_in.skip(_record_info_struct_list[it*step].compressed_size_accumulator+_record_block_offset+_number_width*4+_num_record_blocks*2*_number_width);
		            int jiaX=0;
		            if(it==split_recs_thread_number-1) jiaX=yuShu;
	            	for(int i=it*step; i<it*step+step+jiaX; i++)//_num_record_blocks 
	            	{
	            		if(searchCancled) { poolEUSize=0; return; }
	                    record_info_struct RinfoI = _record_info_struct_list[i];
	                    
	                    int compressed_size = (int) RinfoI.compressed_size;
	                    int decompressed_size = (int) RinfoI.decompressed_size;
	                    data_in.read(record_block_compressed,0, compressed_size);//,0, compressed_size
	                    
	                    //解压开始
	                    if(compareByteArrayIsPara(record_block_compressed,0,_zero4)){
	                        System.arraycopy(record_block_compressed, 8, record_block_, 0, compressed_size-8);
	                    }
	                    else if(compareByteArrayIsPara(record_block_compressed,0,_1zero3)){
	                        //MInt len = new MInt((int) decompressed_size);
	                        //byte[] arraytmp = new byte[ compressed_size];
	                        //System.arraycopy(record_block_compressed, 8, arraytmp, 0, (compressed_size-8));
	                        //MiniLZO.lzo1x_decompress(arraytmp,(int) compressed_size,record_block_,len);
	                        new LzoDecompressor1x().decompress(record_block_compressed, 8, (compressed_size-8), record_block_, 0, new lzo_uintp());
	                    }
	                    else if(compareByteArrayIsPara(record_block_compressed,0,_2zero3)){    
	                        Inflater inf = new Inflater();
	                        inf.setInput(record_block_compressed,8,compressed_size-8);
	                        int ret = inf.inflate(record_block_,0,decompressed_size);  		
	                    	//CMN.show("asdasd"+ret);		
	                    }
	                    //内容块解压完毕
                    	long off = RinfoI.decompressed_size_accumulator;
                    	int key_block_id = binary_find_closest(keyBlocksHeaderTextKeyID,off);
                    	OUT:
                    	while(true) {
                    		if(key_block_id>=_key_block_info_list.length) break;
                    		cached_key_block infoI_cacheI = prepareItemByKeyInfo(null,key_block_id,null);
                    		long[] ko = infoI_cacheI.key_offsets;
                    		//show("binary_find_closest "+binary_find_closest(ko,off)+"  :  "+off);
	                    	for(int relative_pos=binary_find_closest(ko,off);relative_pos<ko.length;relative_pos++) {
	                    		
	                    		
	                    		int recordodKeyLen = 0;
		                    	if(relative_pos<ko.length-1){//不是最后一个entry
		                    		recordodKeyLen=(int) (ko[relative_pos+1]-ko[relative_pos]);
		                    	}//else {
		                    	//	recordodKeyLen = (int) (prepareItemByKeyInfo(null,key_block_id+1,null).key_offsets[0]-ko[ko.length-1]);
		                    	//}
		                    	
		                    	else if(key_block_id<keyBlocksHeaderTextKeyID.length-1){//不是最后一块key block
		                    		recordodKeyLen=(int) (keyBlocksHeaderTextKeyID[key_block_id+1]-ko[relative_pos]);
		                    	}else {
		                    		recordodKeyLen = (int) (decompressed_size-(ko[ko.length-1]-RinfoI.decompressed_size_accumulator));
		                    	}
		                    	
		                    	
		                    	//show(getEntryAt((int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator),infoI_cacheI));
		                    	//CMN.show(record_block_.length-1+" ko[relative_pos]: "+ko[relative_pos]+" recordodKeyLen: "+recordodKeyLen+" end: "+(ko[relative_pos]+recordodKeyLen-1));
		                    	
		                    	/*
		                    	if(getEntryAt((int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator),infoI_cacheI).equals("鼓钟"))
		                    	{		                    	
		                    		CMN.show("decompressed_size: "+decompressed_size+" record_block_: "+(record_block_.length-1)+" ko[relative_pos]: "+ko[relative_pos]+" recordodKeyLen: "+recordodKeyLen+" end: "+(ko[relative_pos]+recordodKeyLen-1));

			                    	CMN.show(flowerIndexOf(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,matcher,0,0)+"");
			                    	

			                    	CMN.show(new String(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator)+248,10,_charset));
			                    	CMN.show(recordodKeyLen+" =recordodKeyLen");
			                    	CMN.show((ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen)+" sdf "+RinfoI.decompressed_size+" sdf "+RinfoI.compressed_size);
			                    	
		                    		CMN.show("\r\n"+new String(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,_charset));

		                    	}*/
		                    	
		                    	if(ko[relative_pos]-RinfoI.decompressed_size_accumulator+recordodKeyLen>RinfoI.decompressed_size) {
		                    		//show("break OUT");
		                    		break OUT;
		                    	}
		                    	
		                    	
		                    	//if(indexOf(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,keys[0],0,keys[0].length,0)!=-1) {
		                    	if(flowerIndexOf(record_block_,(int) (ko[relative_pos]-RinfoI.decompressed_size_accumulator),recordodKeyLen,matcher,0,0)!=-1) {
		                    		int pos = (int) (relative_pos+_key_block_info_list[key_block_id].num_entries_accumulator);
		                    		fuzzyKeyCounter++;
		                    		
		                    		//String LexicalEntry = getEntryAt(pos,infoI_cacheI);
		                    		//show(getEntryAt(pos,infoI_cacheI));
		                    		//ripemd128.printBytes(record_block_,offIdx,recordodKeyLen);
		                    		
		                    		combining_search_tree_4[it].add(pos);
		                    	}
		                    	dirtykeyCounter++;
	                    	}
	                    	key_block_id++;
                    	}
	                }
	            	data_in.close();
	                
	            } catch (Exception e) {e.printStackTrace();}
            	thread_number_count--;
	            if(split_recs_thread_number>thread_number) countDelta(-1);
	        }});
        }
        fixedThreadPool.shutdown();
		try {
			fixedThreadPool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
    }
    
    
	
    /*
     * https://stackoverflow.com/questions/21341027/find-indexof-a-byte-array-within-another-byte-array
     * Gustavo Mendoza's Answer*/
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first)
                    ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++)
                    ;

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
   
	public int split_keys_thread_number;
	//public ArrayList<myCpr<String,Integer>>[] combining_search_tree;
	public ArrayList<Integer>[] combining_search_tree2;
	public ArrayList<Integer>[] combining_search_tree_4;



   public void countDelta(int delta) {
       Lock lock = new ReentrantLock();
       lock.lock();
       try {
           poolEUSize+=delta;
       } catch (Exception e) {
       }finally {
           lock.unlock();
       }
   }
   
   public volatile boolean searchCancled=false;
   public volatile int dirtykeyCounter;
   public volatile static int fuzzyKeyCounter ;
   volatile int poolEUSize;
   
	byte[] keywordArray;
	byte[] keywordArrayC1;
	byte[] keywordArrayCA;
   

  public int thread_number,step,yuShu;
  public void calcFuzzySpace(){
		 //final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
		 //int entryIdx = 0;
		 //show("availableProcessors: "+Runtime.getRuntime().availableProcessors());
		 //show("keyBLockN: "+_key_block_info_list.length);
		 split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
		 split_keys_thread_number = split_keys_thread_number>16?6:split_keys_thread_number;
		 thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+2, split_keys_thread_number);
		 
		
		 thread_number_count = split_keys_thread_number;
		 step = (int) (_num_key_blocks/split_keys_thread_number);
		 yuShu=(int) (_num_key_blocks%split_keys_thread_number);
	 	 
	}
  
	//XXX2
	public void flowerFindAllKeys(String key,
	        final int SelfAtIdx,int theta) 
		{		
		  if(_key_block_info_list==null) read_key_block_info();
	
	
	  	key = key.toLowerCase();
		String upperKey = key.toUpperCase();
	  	final byte[][][] matcher = new byte[upperKey.equals(key)?1:2][][];
		matcher[0] = flowerSanLieZhi(key);
		if(matcher.length==2)
		matcher[1] = flowerSanLieZhi(upperKey);
	  	
	   //final String fkeyword = keyword.toLowerCase().replaceAll(replaceReg,emptyStr);
	   //int entryIdx = 0;
	   show("availableProcessors: "+Runtime.getRuntime().availableProcessors());
	   show("keyBLockN: "+_key_block_info_list.length);
	   split_keys_thread_number = _num_key_blocks<6?1:(int) (_num_key_blocks/6);//Runtime.getRuntime().availableProcessors()/2*2+10;
	   final int thread_number = Math.min(Runtime.getRuntime().availableProcessors()/2*2+5, split_keys_thread_number);
	   
	   poolEUSize = dirtykeyCounter =0;
	   
	   thread_number_count = split_keys_thread_number;
	   final int step = (int) (_num_key_blocks/split_keys_thread_number);
		  final int yuShu=(int) (_num_key_blocks%split_keys_thread_number);
	
		  ExecutorService fixedThreadPoolmy = Executors.newFixedThreadPool(thread_number);
		   
		  show("~"+step+"~"+split_keys_thread_number+"~"+_num_key_blocks);
		  if(combining_search_tree2==null)
			  combining_search_tree2 = new ArrayList[split_keys_thread_number];
		  
	   for(int ti=0; ti<split_keys_thread_number; ti++){//分  thread_number 股线程运行
		   		if(searchCancled) break;
		        if(split_keys_thread_number>thread_number) while (poolEUSize>=thread_number) {  
		              try {
		    			Thread.sleep(1);
			    		} catch (InterruptedException e) {
			    			e.printStackTrace();
			    		}  
		        } 
		        if(split_keys_thread_number>thread_number) countDelta(1);
		    	final int it = ti;
		        fixedThreadPoolmy.execute(
		        new Runnable(){@Override public void run() 
		        {
		        	if(searchCancled) { poolEUSize=0; return; }
		            int jiaX=0;
		            if(it==split_keys_thread_number-1) jiaX=yuShu;
		            final byte[] key_block = new byte[65536];/*分配资源 32770   65536*/
		            if(combining_search_tree2[it]==null)
		            	combining_search_tree2[it] = new ArrayList<Integer>();
	           	
		            
		            int compressedSize_many = 0;
		           //小循环	
		            for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++){
		                   //prepareItemByKeyInfo(_key_block_info_list[blockCounter],blockCounter);
		                   key_info_struct infoI = _key_block_info_list[blockId];
		                   if(blockId==_key_block_info_list.length-1)
		                	   compressedSize_many += _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
		                   else
		                	   compressedSize_many += _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
		            }
		            
	            long start = _key_block_info_list[it*step].key_block_compressed_size_accumulator;
	
	            try {
						DataInputStream data_in = getStreamAt(_key_block_offset+start);
						
						byte[]  _key_block_compressed_many = new byte[ compressedSize_many];
						data_in.read(_key_block_compressed_many, 0, _key_block_compressed_many.length);
						data_in.close();
						
						//大循环	
						for(int blockId=it*step; blockId<it*step+step+jiaX; blockId++){
							if(searchCancled) { poolEUSize=0; return; }
							
							int compressedSize;
							key_info_struct infoI = _key_block_info_list[blockId];
							if(blockId==_key_block_info_list.length-1)
								compressedSize = (int) (_key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator);
							else
								compressedSize = (int) (_key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator);
							
							int startI = (int) (infoI.key_block_compressed_size_accumulator-start);
							   
							
							//byte[] record_block_type = new byte[]{_key_block_compressed_many[(int) startI],_key_block_compressed_many[(int) (startI+1)],_key_block_compressed_many[(int) (startI+2)],_key_block_compressed_many[(int) (startI+3)]};
							//int adler32 = getInt(_key_block_compressed_many[(int) (startI+4)],_key_block_compressed_many[(int) (startI+5)],_key_block_compressed_many[(int)(startI+6)],_key_block_compressed_many[(int) (startI+7)]);
		
							if(compareByteArrayIsPara(_key_block_compressed_many,startI,_zero4)){
								  System.arraycopy(_key_block_compressed_many, (startI+8), key_block, 0, (int)(_key_block_size-8));
							}else if(compareByteArrayIsPara(_key_block_compressed_many,startI,_1zero3))
							{
								  //MInt len = new MInt();//(int) infoI.key_block_decompressed_size
								  //byte[] arraytmp = new byte[(int) compressedSize];
								  //System.arraycopy(_key_block_compressed_many, (startI+8), arraytmp, 0, (compressedSize-8));
								  //MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block,len);
					              new LzoDecompressor1x().decompress(_key_block_compressed_many, startI+8, (int)(compressedSize-8), key_block, 0,new lzo_uintp());
							}
							else if(compareByteArrayIsPara(_key_block_compressed_many,startI,_2zero3))
							{
									//byte[] key_block2 = zlib_decompress(_key_block_compressed_many,(int) (startI+8),(int)(compressedSize-8));
									//System.arraycopy(key_block2, 0, key_block, 0, key_block2.length);
									//find_in_keyBlock(key_block2,infoI,keyword,SelfAtIdx,it);
									
									Inflater inf = new Inflater();
									//CMN.show(_key_block_compressed_many.length+";;"+(startI+8)+";;"+(compressedSize-8));
									inf.setInput(_key_block_compressed_many,(startI+8),(compressedSize-8));
									//key_block = new byte[(int) infoI.key_block_decompressed_size];
									try {
									  //CMN.show(""+infoI.key_block_decompressed_size);
										int ret = inf.inflate(key_block,0,(int)(infoI.key_block_decompressed_size));
									} catch (DataFormatException e) {e.printStackTrace();}
							}
							
							find_in_keyBlock(key_block,infoI,matcher,SelfAtIdx,it);
							
							
							
						} 
		            }
	            catch (Exception e1) {e1.printStackTrace();}
	           thread_number_count--;
	           if(split_keys_thread_number>thread_number) countDelta(-1);
		        }});
	   }//任务全部分发完毕
		fixedThreadPoolmy.shutdown();
		try {
			fixedThreadPoolmy.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}  


	HashSet<Integer> miansi = new HashSet<>();//. is 免死金牌  that exempt you from death for just one time
	HashSet<Integer> yueji = new HashSet<>();//* is 越级天才, i.e., super super genius leap

	int flowerIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers,int marcherOffest, int fromIndex) throws UnsupportedEncodingException 
	{
		int lastSeekLetSize=0;
		while(fromIndex<sourceCount) {
			//CMN.show("==");
			//int idx = -1;
			int fromIndex_=fromIndex;
			boolean isSeeking=true;
			boolean Matched = false;
			for(int lexiPartIdx=marcherOffest;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
				//if(fromIndex_>sourceCount-1) return -1;
				//CMN.show("stst: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx);
		
				//CMN.show("seekPos: "+seekPos+" lexiPartIdx: "+lexiPartIdx+" fromIndex_: "+fromIndex_);
				if(miansi.contains(lexiPartIdx)) {
					if(lexiPartIdx==matchers[0].length-1) {
						if(fromIndex_>=sourceCount)
							return -1;
						return fromIndex-lastSeekLetSize;//HERE
					}//Matched=true
					//CMN.show("miansi: "+lexiPartIdx);
					//CMN.show("miansi: "+sourceCount+"::"+(fromIndex_+seekPos)+"sourceL: "+source.length);
					//CMN.show("jumpped c is: "+new String(source,fromIndex_+seekPos,Math.min(4, sourceCount-(fromIndex_+seekPos-sourceOffset)),_encoding).substring(0, 1));
					int newSrcCount = Math.min(4, sourceCount-(fromIndex_));
					if(newSrcCount<=0)
						return -1;
					String c = new String(source,sourceOffset+fromIndex_,newSrcCount,_charset);
					int jumpShort = c.substring(0, 1).getBytes(_charset).length;
					fromIndex_+=jumpShort;
					continue;
				}else if(yueji.contains(lexiPartIdx)) {
					if(lexiPartIdx==matchers[0].length-1) 
						return fromIndex-lastSeekLetSize;//HERE
					if(flowerIndexOf(source, sourceOffset+fromIndex_,sourceCount-(fromIndex_), matchers,lexiPartIdx+1, 0)!=-1){
						return fromIndex-lastSeekLetSize;
					}
					return -1;
				}
				Matched = false;
				if(isSeeking) {		
					int seekPos=-1;
					int newSeekPos=-1;
					for(byte[][] marchLet:matchers) {
		    			//if(marchLet==null) break;
		    			if(newSeekPos==-1)
		    				newSeekPos = indexOf(source, sourceOffset, sourceCount, marchLet[lexiPartIdx],0,marchLet[lexiPartIdx].length, fromIndex_) ;
		    			else        				
		    				newSeekPos = indexOf(source, sourceOffset, newSeekPos, marchLet[lexiPartIdx],0,marchLet[lexiPartIdx].length, fromIndex_) ;
		    			//Lala=MinimalIndexOf(source, sourceOffset, sourceCount, new byte[][] {matchers[0][lexiPartIdx],matchers[1][lexiPartIdx]},0,-1,fromIndex_+seekPos);
		    			if(newSeekPos!=-1) {
		    				seekPos=newSeekPos;
							lastSeekLetSize=matchers[0][lexiPartIdx].length;
							Matched=true;
						}
					}
					//CMN.show("seekPos:"+seekPos+" fromIndex_: "+fromIndex_);
					if(!Matched)
						return -1;
					seekPos+=lastSeekLetSize;
					fromIndex=fromIndex_=seekPos;
					isSeeking=false;
					continue;
					}
				else {
					//CMN.show("deadline"+fromIndex_+" "+sourceCount);
					if(fromIndex_>sourceCount-1) {
						//CMN.show("deadline reached"+fromIndex_+" "+sourceCount);
						return -1;
					}
					for(byte[][] marchLet:matchers) {
						if(marchLet==null) break;
						if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex_)) {
							Matched=true;
			    			//CMN.show("matchedHonestily: "+sourceCount+"::"+(fromIndex_+seekPos)+" fromIndex_: "+fromIndex_+" seekPos: "+seekPos);
							//CMN.show("matchedHonestily: "+lexiPartIdx);
						}
					}
				}
				if(!Matched) {
					//CMN.show("Matched failed this round: "+lexiPartIdx);
					break;
				}
				fromIndex_+=matchers[0][lexiPartIdx].length;
			}
			if(Matched)
				return fromIndex-lastSeekLetSize;
		}
		return -1;
	}


	private byte[][] flowerSanLieZhi(String str) {
		miansi.clear();
		yueji.clear();
		byte[][] res = new byte[str.length()][];
		for(int i=0;i<str.length();i++){
			String c = str.substring(i, i+1);
			if(c.equals("."))
				miansi.add(i);
			else if(c.equals("*"))
				yueji.add(i);
			else
				res[i] = c.getBytes(_charset);
		}
		return res;
	}
	 
 
    protected void find_in_keyBlock(byte[] key_block,key_info_struct infoI,byte[][][] matcher,int SelfAtIdx,int it) {
	 //!!spliting curr Key block
       int key_start_index = 0;
       //String delimiter;
       int key_end_index=0;
       //int keyCounter = 0;
       
       //ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
       int keyCounter = 0;
       while(key_start_index < infoI.key_block_decompressed_size){
     	  //long key_id;
           //if(_version<2)
        	     // sf.position(4);
               //key_id = sf.getInt(key_start_index);//Key_ID
           //else
        	   	 // sf.position(8);
               //key_id = sf.getLong(key_start_index);//Key_ID
           //show("key_id"+key_id);
           key_end_index = key_start_index + _number_width;  

           SK_DELI:
		  while(true){
			for(int sker=0;sker<delimiter_width;sker++) {
				if(key_block[key_end_index+sker]!=0) {
					key_end_index+=delimiter_width;
					continue SK_DELI;
				}
			}
			break;
		  }
           
           if(true)
		try {
			//TODO: alter
			//xxxx
			int try_idx = flowerIndexOf(key_block,key_start_index+_number_width, key_end_index-(key_start_index+_number_width), matcher,0,0);

			
			if(try_idx!=-1){
				//复核 re-collate
				String LexicalEntry = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset);
				//int LexicalEntryIdx = LexicalEntry.toLowerCase().indexOf(keyword.toLowerCase());
	         	//if(LexicalEntryIdx==-1) {
	         	//	key_start_index = key_end_index + width;
	         	//	dirtykeyCounter++;continue;
	         	//}
				//StringBuilder sb = new StringBuilder(LexicalEntry);
	         	//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
	         	//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
				//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
				//tmpnode.value.add(SelfAtIdx);
				//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
				combining_search_tree2[it].add((int) (infoI.num_entries_accumulator+keyCounter));//new additiveMyCpr1(LexicalEntry,infoI.num_entries_accumulator+keyCounter));

	         	fuzzyKeyCounter++;
         }
		} catch (Exception e) {
			e.printStackTrace();
		}


           key_start_index = key_end_index + delimiter_width;
           keyCounter++;dirtykeyCounter++;
       }
       //assert(adler32 == (calcChecksum(key_block)));	
}

   protected void find_in_keyBlock(byte[] key_block,key_info_struct infoI,String keyword,int SelfAtIdx,int it) {
	 //!!spliting curr Key block
       int key_start_index = 0;
       int key_end_index=0;
       //int keyCounter = 0;
       
       //ByteBuffer sf = ByteBuffer.wrap(key_block);//must outside of while...
       int keyCounter = 0;
       while(key_start_index < infoI.key_block_decompressed_size){
     	   //long key_id;
           //if(_version<2)
        	      //sf.position(4);
               //key_id = sf.getInt(key_start_index);//Key_ID
          // else
        	   	  //sf.position(8);
               //key_id = sf.getLong(key_start_index);//Key_ID
           //show("key_id"+key_id);
           
               key_end_index = key_start_index + _number_width;  
               SK_DELI:
     		  while(true){
     			for(int sker=0;sker<delimiter_width;sker++) {
     				if(key_block[key_end_index+sker]!=0) {
     					key_end_index+=delimiter_width;
     					continue SK_DELI;
     				}
     			}
     			break;
     		  }
               
     if(true)
		try {
			//TODO: alter
			//xxxx
			//if(new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_encoding).toLowerCase().indexOf(keyword.toLowerCase())!=-1) {
			int try_idx = indexOf(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),keywordArray,0,keywordArray.length,0);
			if(try_idx==-1)
				try_idx = indexOf(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),keywordArrayC1,0,keywordArray.length,0);
			if(try_idx==-1 && keyword.length()>0)
				try_idx = indexOf(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),keywordArrayCA,0,keywordArray.length,0);
         
			if(try_idx!=-1){
				//复核 re-collate
				String LexicalEntry = new String(key_block,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset);
				int LexicalEntryIdx = LexicalEntry.toLowerCase().indexOf(keyword.toLowerCase());
	         	if(LexicalEntryIdx==-1) {
	         		key_start_index = key_end_index + delimiter_width;
	         		dirtykeyCounter++;continue;
	         	}
	         	
				//StringBuilder sb = new StringBuilder(LexicalEntry);
	         	//byte[] arraytmp = new byte[key_end_index-(key_start_index+_number_width)];
	         	//System.arraycopy(key_block,key_start_index+_number_width, arraytmp, 0,arraytmp.length);
				//additiveMyCpr1 tmpnode = new additiveMyCpr1(LexicalEntry,""+SelfAtIdx+""+((int) (infoI.num_entries_accumulator+keyCounter)));//new ArrayList<Integer>() new int[] {SelfAtIdx,(int) (infoI.num_entries_accumulator+keyCounter)}
				//tmpnode.value.add(SelfAtIdx);
				//tmpnode.value.add((int) (infoI.num_entries_accumulator+keyCounter));
				combining_search_tree2[it].add((int) (infoI.num_entries_accumulator+keyCounter));//new additiveMyCpr1(LexicalEntry,));

	         	fuzzyKeyCounter++;
         }
		} catch (Exception e) {
			e.printStackTrace();
		}


           key_start_index = key_end_index + delimiter_width;
           keyCounter++;dirtykeyCounter++;
       }
       //assert(adler32 == (calcChecksum(key_block)));	
   }

    
  
	byte[] key_block_cache = null;
	int key_block_cacheId=-1;
	int key_block_Splitted_flag=-1;


	int[][] scaler;
	public ArrayList<myCpr<String, Integer>> combining_search_list;
  //联合搜索  555
	public void size_confined_lookUp5(String keyword,
	        RBTree_additive combining_search_tree, int SelfAtIdx, int theta) 
			{
		ArrayList<myCpr<String, Integer>> _combining_search_list = combining_search_list;
		keyword = processText(keyword);
		byte[] kAB = keyword.getBytes(_charset);
		if(_encoding.startsWith("GB")) {
			if(compareByteArray(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,kAB)<0)
				return;
			if(compareByteArray(_key_block_info_list[0].headerKeyText,kAB)>0)
				return;
		}else {
			if(new String(_key_block_info_list[(int)_num_key_blocks-1].tailerKeyText,_charset).compareTo(keyword)<0)
				return;
			if(new String(_key_block_info_list[0].headerKeyText,_charset).compareTo(keyword)>0)
				return;
		}
		if(_key_block_info_list==null) read_key_block_info();
	    int blockId = _encoding.startsWith("GB")?reduce2(kAB,0,_key_block_info_list.length):reduce(keyword,0,_key_block_info_list.length);
		if(blockId==-1) return;
		//show(_Dictionary_fName+_key_block_info_list[blockId].tailerKeyText+"1~"+_key_block_info_list[blockId].headerKeyText);
		//while(blockId!=0 &&  compareByteArray(_key_block_info_list[blockId-1].tailerKeyText,kAB)>=0)
		//	blockId--;
	
		boolean doHarvest=false;
		
		//OUT:
		while(theta>0) {
			key_info_struct infoI = _key_block_info_list[blockId];
			try {
				long start = infoI.key_block_compressed_size_accumulator;
				long compressedSize;
				if(!(key_block_cacheId==blockId && key_block_cache!=null)) {
					if(blockId==_key_block_info_list.length-1)
						compressedSize = _key_block_size - _key_block_info_list[_key_block_info_list.length-1].key_block_compressed_size_accumulator;
					else
						compressedSize = _key_block_info_list[blockId+1].key_block_compressed_size_accumulator-infoI.key_block_compressed_size_accumulator;
		             
		  			DataInputStream data_in = getStreamAt(_key_block_offset+start);
		  					
		  			byte[]  _key_block_compressed = new byte[(int) compressedSize];
		  			data_in.read(_key_block_compressed, (int)(0), _key_block_compressed.length);
		  			data_in.close();
		  			
		            //int adler32 = getInt(_key_block_compressed[(int) (+4)],_key_block_compressed[(int) (+5)],_key_block_compressed[(int) (+6)],_key_block_compressed[(int) (+7)]);
					if(compareByteArrayIsPara(_zero4, _key_block_compressed)){
						//System.out.println("no compress!");
						key_block_cache = new byte[(int) (_key_block_compressed.length-start-8)];
						System.arraycopy(_key_block_compressed, (int)(start+8), key_block_cache, 0,key_block_cache.length);
					}else if(compareByteArrayIsPara(_1zero3, _key_block_compressed))
					{
						//MInt len = new MInt((int) infoI.key_block_decompressed_size);
						//key_block_cache = new byte[len.v];
						//byte[] arraytmp = new byte[(int) compressedSize];
						//System.arraycopy(_key_block_compressed, (int)(+8), arraytmp, 0,(int) (compressedSize-8));
						//MiniLZO.lzo1x_decompress(arraytmp,arraytmp.length,key_block_cache,len);
						key_block_cache =  new byte[(int) infoI.key_block_decompressed_size];
	                    new LzoDecompressor1x().decompress(_key_block_compressed, 8, (int)(compressedSize-8), key_block_cache, 0, new lzo_uintp());
					}
					else if(compareByteArrayIsPara(_2zero3, _key_block_compressed)){
						//key_block_cache = zlib_decompress(_key_block_compressed,(int) (+8),(int)(compressedSize-8));
						key_block_cache =  new byte[(int) infoI.key_block_decompressed_size];
						Inflater inf = new Inflater();
	                    inf.setInput(_key_block_compressed,8,(int)compressedSize-8);
	                    int ret = inf.inflate(key_block_cache,0,key_block_cache.length);  
					}
					key_block_cacheId = blockId;
				}
				/*!!spliting curr Key block*/
				if(key_block_Splitted_flag!=blockId) {
					if(!doHarvest)
						scaler = new int[(int) infoI.num_entries][2];
					int key_start_index = 0;
					int key_end_index=0;
					int keyCounter = 0;
					
					while(key_start_index < key_block_cache.length){
						  key_end_index = key_start_index + _number_width;
						  SK_DELI:
						  while(true){
							for(int sker=0;sker<delimiter_width;sker++) {
								if(key_block_cache[key_end_index+sker]!=0) {
									key_end_index+=delimiter_width;
									continue SK_DELI;
								}
							}
							break;
						  }
						//CMN.show(new String(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),_charset));
						//if(EntryStartWith(key_block_cache,key_start_index+_number_width,key_end_index-(key_start_index+_number_width),matcher)) {
						if(doHarvest) {
							String kI = new String(key_block_cache, key_start_index+_number_width,key_end_index-(key_start_index+_number_width), _charset);
							if(processText(kI).startsWith(keyword)) {
								if(combining_search_tree!=null)
									combining_search_tree.insert(kI,SelfAtIdx,(int)(keyCounter+infoI.num_entries_accumulator));
								else
									_combining_search_list.add(new myCpr(kI,(int)(keyCounter+infoI.num_entries_accumulator)));
								theta--;
							}else return;
							if(theta<=0) return;
						}else {
							scaler[keyCounter][0] = key_start_index+_number_width;
							scaler[keyCounter][1] = key_end_index-(key_start_index+_number_width);
						}
		
						key_start_index = key_end_index + delimiter_width;
						keyCounter++;
					}
					if(!doHarvest) key_block_Splitted_flag=blockId;
				}
	  			} catch (Exception e2) {
	  				e2.printStackTrace();
	  			}
			
			
			if(!doHarvest) {
				int idx;
				if(_encoding.startsWith("GB"))
					idx = reduce2(kAB, key_block_cache, scaler, 0, (int) infoI.num_entries);
				else
					idx = reduce(keyword, key_block_cache, scaler, 0, (int) infoI.num_entries);
				//CMN.show(new String(key_block_cache, scaler[idx][0],scaler[idx][1], _charset));
				//CMN.show(new String(key_block_cache, scaler[idx+1][0],scaler[idx+1][1], _charset));
				
				String kI = new String(key_block_cache, scaler[idx][0],scaler[idx][1], _charset);
				while(true) {
					if(processText(kI).startsWith(keyword)) {
						if(combining_search_tree!=null)
							combining_search_tree.insert(kI,SelfAtIdx,(int)(idx+infoI.num_entries_accumulator));
						else
							_combining_search_list.add(new myCpr<String,Integer>(kI,(int)(idx+infoI.num_entries_accumulator)));
						theta--;
					}else
						return;
					idx++;
					//if(idx>=infoI.num_entries) CMN.show("nono!");
					if(theta<=0)
						return;
					if(idx>=infoI.num_entries) {
						break;
					}
					kI = new String(key_block_cache, scaler[idx][0],scaler[idx][1], _charset);
				}
				doHarvest=true;
			}
			++blockId;
			if(_key_block_info_list.length<=blockId) return;
		}
	}
	public int reduce(String phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
	    int len = end-start;
	    if (len > 1) {
	      len = len >> 1;
		  //int iI = start + len - 1;
	      return phrase.compareTo(processText(new String(data, scaler[start + len - 1][0], scaler[start + len - 1][1], _charset)))>0
	                ? reduce(phrase,data,scaler,start+len,end)
	                : reduce(phrase,data,scaler,start,start+len);
	    } else {
	      return start;
	    }
	}
	
	public int reduce2(byte[] phrase,byte[] data,int[][] scaler,int start,int end) {//via mdict-js
	    int len = end-start;
	    if (len > 1) {
	      len = len >> 1;
		  //int iI = start + len - 1;
		  byte[] sub_data = processText(new String(data, scaler[start + len - 1][0], scaler[start + len - 1][1], _charset)).getBytes(_charset);
	      return compareByteArray(phrase, sub_data)>0
	                ? reduce2(phrase,data,scaler,start+len,end)
	                : reduce2(phrase,data,scaler,start,start+len);
	    } else {
	      return start;
	    }
	}


	
	
	
	
	
	
	
	
    public int  binary_find_closest(byte[][] array,String val){
    	if(array==null)
    		return -1;
    	int iLen = array.length;
    	if(iLen<1)
    		return -1;
    	
		//System.out.println(new String(array[0])+":"+new String(array[array.length-1]));
		int boundaryCheck = val.compareTo(processText(new String(array[0],_charset)));
    	if(boundaryCheck<0){
    		return -1;
    	}else if(boundaryCheck==0)
			return 0;
    	boundaryCheck = val.compareTo(processText(new String(array[iLen-1],_charset)));
    	if(boundaryCheck>0){
    		return -1;
    	}else if(boundaryCheck==0)
			return iLen-1;
    	
    	int resPreFinal = reduce_keys(array,val,0,array.length);
    	return resPreFinal;
    }
    //binary_find_closest: with_charset! with_charset! with_charset!
    public int  binary_find_closest2(byte[][] array,String val) {
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	//if(iLen==1)
    	//	return 0;
    	
    	byte[] valBA = val.getBytes(_charset);
    	
    	int boundaryCheck = compareByteArray(valBA,processKey(array[0]).getBytes(_charset));
    	if(boundaryCheck<0){
			return -1;
    	}else  if(boundaryCheck==0) {return 0;}
    	
    	boundaryCheck = compareByteArray(valBA,processKey(array[iLen-1]).getBytes(_charset));
    	if(boundaryCheck>0){
    			return -1;
    	}else if(boundaryCheck==0) {return iLen-1;}

    	return reduce_keys2(array,valBA,0,array.length);
    }
    
    public String processKey(byte[] in){
    	return processText(new String(in,_charset));
    }
    
        

    
    public static int  binary_find_closest(long[] array,long val){
    	int middle = 0;
    	int iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>=0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		counter+=1;
    		//System.out.println(low+":"+high);
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
        		high=middle;
        	}
    	}
		return low;
    }
    
    
 
    
    
    
    

    
	public static short getShort(byte buf1, byte buf2) 
    {
        short r = 0;
        r |= (buf1 & 0x00ff);
        r <<= 8;
        r |= (buf2 & 0x00ff);
        return r;
    }
    
    public static int getInt(byte buf1, byte buf2, byte buf3, byte buf4) 
    {
        int r = 0;
        r |= (buf1 & 0x000000ff);
        r <<= 8;
        r |= (buf2 & 0x000000ff);
        r <<= 8;
        r |= (buf3 & 0x000000ff);
        r <<= 8;
        r |= (buf4 & 0x000000ff);
        return r;
    }
    public static long getLong(byte[] buf) 
    {
        long r = 0;
        r |= (buf[0] & 0xff);
        r <<= 8;
        r |= (buf[1] & 0xff);
        r <<= 8;
        r |= (buf[2] & 0xff);
        r <<= 8;
        r |= (buf[3] & 0xff);
        r <<= 8;
        r |= (buf[4] & 0xff);
        r <<= 8;
        r |= (buf[5] & 0xff);
        r <<= 8;
        r |= (buf[6] & 0xff);
        r <<= 8;
        r |= (buf[7] & 0xff);
        return r;
    }

    public static String byteTo16(byte bt){
        String[] strHex={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
        String resStr=emptyStr;
        int low =(bt & 15);
        int high = bt>>4 & 15;
        resStr = strHex[high]+strHex[low];
        return resStr;
    }
    
    
    public String getDictInfo(){
    	return new StringBuilder()
    			.append("Engine Version: ").append(_version).append("<BR>")
    			.append("CreationDate: ").append((_header_tag.containsKey("CreationDate")?_header_tag.get("CreationDate"):"UNKNOWN")).append("<BR>")
    			.append("Charset &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; : ").append(this._encoding).append("<BR>")
    			.append("Num Entries: ").append(this._num_entries).append("<BR>")
    			.append("Num Key Blocks: ").append(this._num_key_blocks).append("<BR>")
    			.append("Num Rec Blocks: ").append(this._num_record_blocks).append("<BR>")
    			.append(mdd==null?"&lt;no assiciated mdRes&gt;":("MdResource count "+mdd.getNumberEntries()+","+mdd._encoding+","+mdd._num_key_blocks+","+mdd._num_record_blocks)).append("<BR>")
    			.append("Path: ").append(this.getPath()).toString();
    }
    
    static boolean EntryStartWith(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers) {
		boolean Matched = false;
		int fromIndex=0;
    	for(int lexiPartIdx=0;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
    		Matched = false;
    		for(byte[][] marchLet:matchers) {
    			if(marchLet==null) break;
    			if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex)) {
    				Matched=true;
    			}
    		}
    		if(!Matched)
    			return false;
    		fromIndex+=matchers[0][lexiPartIdx].length;
    	}
    	return true;
    }

    private byte[][] SanLieZhi(String str) {
    	byte[][] res = new byte[str.length()][];
    	for(int i=0;i<str.length();i++){
    		String c = str.substring(i, i+1);
    		res[i] = c.getBytes(_charset);
		}
		return res;
	}

    static boolean bingStartWith(byte[] source, int sourceOffset,byte[] target, int targetOffset, int targetCount, int fromIndex) {
    	if (fromIndex >= source.length) {
    		return false;
        }
    	if(targetCount<=-1)
    		targetCount=target.length;
    	if(sourceOffset+targetCount>=source.length)
        	return false;
    	for (int i = sourceOffset + fromIndex; i <= sourceOffset+fromIndex+targetCount-1; i++) {
    		if (source[i] != target[targetOffset+i-sourceOffset-fromIndex]) 
    			return false;
    	}
    	return true;
    }


    public static String processText(String input) {
 		return replaceReg.matcher(input).replaceAll(emptyStr).toLowerCase();
 	}
    
    public String processStyleSheet(String input) {
    	if(_stylesheet.size()==0)
    		return input;
 		Matcher m = markerReg.matcher(input);
 		//HashSet<String> Already = new HashSet<>();
 		StringBuilder transcriptor = new StringBuilder();
 		String last=null;
 		int lastEnd=0;
 		boolean returnRaw=true;
 		while(m.find()) {
			String now = m.group(1);
			String[] nowArr = _stylesheet.get(now);
			if(nowArr==null) {
				if(last!=null) {
					transcriptor.append(last);
					last=null;
				}
				continue;
			}
 			transcriptor.append(input.substring(lastEnd, m.start()));
 			if(last!=null) transcriptor.append(last);
 			transcriptor.append(StringEscapeUtils.unescapeHtml(nowArr[0]));
 			lastEnd = m.end();
			last = StringEscapeUtils.unescapeHtml(nowArr[1]);
			returnRaw=false;
	    }
 		if(returnRaw)
 			return input;
 		else
 			return transcriptor.append(last==null?"":last).append(input.substring(lastEnd,input.length())).toString();
 	}
    
}


 