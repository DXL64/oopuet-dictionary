package com.knziha.plod.dictionaryBuilder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Deflater;

import org.anarres.lzo.LzoCompressor1x_1;
import org.anarres.lzo.lzo_uintp;
import org.apache.commons.text.StringEscapeUtils;

import com.knziha.plod.dictionary.BU;
import com.knziha.plod.dictionary.key_info_struct;
import com.knziha.plod.dictionary.myCpr;
import com.knziha.plod.dictionary.record_info_struct;
import com.knziha.rbtree.RBTNode;
import com.knziha.rbtree.RBTree_duplicative;
import com.knziha.rbtree.RBTree_duplicative.inOrderDo;

import test.CMN;

/**
 * @author KnIfER
 * @date 2018/05/31
 */

public class mdictBuilder{
    	private String _Dictionary_Name;
    	private String _about;
		String _encoding="";
	    private int _encrypt=0;
	    private int grossCompressionType=1;
		//private String _passcode = "";
		public String _stylesheet = "";
		private float _version=2.0f;
		private long _num_entries;public long getNumberEntries(){return _num_entries;}
		private long _num_key_blocks;
	    private long _num_record_blocks;
	    
	    private final static String emptyStr = "";
	    
	    private key_info_struct[] _key_block_info_list;
	    
	    
	    public ArrayListTree<myCpr<String, String>> data_tree;
	    public HashMap<Integer,Integer> privateZone;
	    public mdictBuilder(String Dictionary_Name,
	    		String about,
	    		String codec
	    		) {
	    	data_tree=new ArrayListTree<myCpr<String, String>>();
	    	privateZone = new HashMap<>();
	    	_Dictionary_Name=Dictionary_Name;
	    	_about=StringEscapeUtils.escapeHtml4(about);
	    	_encoding=codec;
	    }
	    
	    public HashMap<String,File> fileTree = new HashMap<>();
	    
	    public int insert(String key,String data) {
	    	data_tree.insert(new myCpr<>(key,data));
	    	return 0;
	    }

		
	    private final String nullStr=null;
	    
		public void insert(String key, File inhtml) {
	    	data_tree.insert(new myCpr<>(key,nullStr));
			fileTree.put(key, inhtml);
		}
		public void append(String key, File inhtml) {
	    	data_tree.add(new myCpr<>(key,nullStr));
			fileTree.put(key, inhtml);
		}
	    private String constructHeader() {
	    	String encoding = _encoding;
	        if(encoding.equals("UTF-16LE"))
	        	encoding = "UTF-16"; //INCONGRUENT java charset          
	        if (encoding.equals(""))
	        	encoding = "UTF-8";
	    	StringBuilder sb = new StringBuilder()
	    			.append("<Dictionary GeneratedByEngineVersion=")//v
	    			.append("\"").append(_version).append("\"")
	    			.append(" RequiredEngineVersion=")//v
	    			.append("\"").append(_version).append("\"")
	    			.append(" Encrypted=")
	    			.append("\"").append("0").append("\"")//is NO valid?
	    			.append(" Encoding=")
	    			.append("\"").append(encoding).append("\"")
	    			.append(" Format=")//Format
	    			.append("\"").append("Html").append("\"")
	    			.append(" Compact=")//c
	    			.append("\"").append("Yes").append("\"")
	    			.append(" KeyCaseSensitive=")//k
	    			.append("\"").append("No").append("\"")
	    			.append(" Description=")
	    			.append("\"").append(_about).append("\"")
	    			.append(" Title=")
	    			.append("\"").append(_Dictionary_Name).append("\"")
	    			.append(" StyleSheet=")
	    			.append("\"").append(_stylesheet).append("\"")
	    			.append("/>");
	    	//sb.append()
			try {
				return new String(new byte[] {(byte) 0xff,(byte) 0xfe},"UTF-16LE")+sb.toString();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
		}
	    
	    File dirP;
	    File index_tmp,record_tmp;
	    public void write(String path) throws IOException {

	    	dirP = new File(path).getParentFile();
	    	dirP.mkdirs();
	    	if(!dirP.exists() && dirP.isDirectory())
				throw new IOException("input path invalid");
	    	
	    	index_tmp = new File(dirP,"index_tmp.mdict");
			record_tmp = new File(dirP,"record_tmp.mdict");
			index_tmp.delete();index_tmp.createNewFile();
			record_tmp.delete();record_tmp.createNewFile();
	    	
	    	DataOutputStream fOut = new DataOutputStream(new  FileOutputStream(path));
	    	// number of bytes of header text

	    	//!!!!
	    	splitKeys();
//![1]write_header 
	    	String headerString = constructHeader();
	    	//CMN.show(headerString);
	    	byte[] header_bytes = headerString.getBytes("UTF-16LE");
	    	fOut.writeInt(header_bytes.length);
	    	fOut.write(header_bytes);
	    	//CMN.show(""+);
	    	fOut.write(BU.toLH(BU.calcChecksum(header_bytes)));
//key block info
	    	ByteBuffer sf = ByteBuffer.wrap(new byte[5*8]);
	    	
	    	/*number of keyblock count*/
	    	sf.putLong(_key_block_info_list.length);
	    	/*number of entries count*/
	    	sf.putLong(_num_entries);
	    	
	    	constructKeyBlockInfoData();
	    	
	    	/*number of bytes of deccompressed key block info data*/
	    	sf.putLong(KeyBlockInfoDataLN2);
	    	/*number of bytes of key block info*/
	    	sf.putLong(KeyBlockInfoDataLN+4*2);
	    	/*number of bytes of key block*/
	    	sf.putLong(key_block_compressed_size_accumulator+8*_key_block_info_list.length);
	    	byte[] five_Number_Bytes = sf.array();
	    	
	    	//CMN.show("key_block_info_size="+(KeyBlockInfoDataLN+4*2));
			//CMN.show("key_block_info_decomp_size="+KeyBlockInfoDataLN2);
			
	    	fOut.write(five_Number_Bytes);
	    	fOut.writeInt(BU.calcChecksum(five_Number_Bytes));
	    	
	    	fOut.write(new byte[]{2,0,0,0});
	    	fOut.writeInt(KeyBlockInfoDataDeCompressedAlder32);//BU.calcChecksum(KeyBlockInfoData,0,(int) KeyBlockInfoDataLN)+4*2);
	    	fOut.write(KeyBlockInfoData);
	    	
	    	/*
	    	for(key_info_struct infoI:_key_block_info_list) {//TODO 会不会爆内存呢？
	    		if(grossCompressionType==1) {
	    			fOut.write(new byte[]{1,0,0,0});
	    	    	//fOut.write(new byte[] {0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,9,9,9,9,9});
    			}
    			fOut.writeInt(BU.calcChecksum(infoI.key_block_data));
    			fOut.write(infoI.key_block_data,0,(int) infoI.key_block_compressed_size);
	    		
	    	}*/
	    	FileInputStream tmpIn = new FileInputStream(index_tmp);  
            byte[] buf=new byte[1024];  
            int n=0;//记录实际读取到的字节数  
            while((n=tmpIn.read(buf))!=-1)  
            {  
                //输出到另一个文件  
            	fOut.write(buf,0,n);  
            }
            tmpIn.close();
	    	
//![3]Encoding_record_block_header
	    	/*numer of record blocks*/
	    	DataOutputStream fOutTmp = new DataOutputStream(new  FileOutputStream(record_tmp));
	    	
	    	int posB = fOutTmp.size();
	    	//写入内容
	    	ArrayList<record_info_struct> eu_RecordblockInfo = new ArrayList<record_info_struct>();
	    	int baseCounter=0;
	    	int cc=0;
	    	for(int blockInfo_L_I:blockInfo_L) {
	    		CMN.show("writing recording block No."+(cc++));
	    		//写入记录块
	    		record_info_struct RinfoI = new record_info_struct();
	    		if(true) {//lzo压缩
	    			//dict=new int[102400];
		    		ByteArrayOutputStream data_raw = new ByteArrayOutputStream();
	    			//CMN.show(blockInfo_L[i]+":"+values.length);
		    		for(int entryC=0;entryC<blockInfo_L_I;entryC++) {//压入内容
		    			byte[] byteContent=null;
		    			if(values[baseCounter+entryC]==null) {
		    				File inhtml = fileTree.get(keys[baseCounter+entryC]);
		    				FileInputStream FIN = new FileInputStream(inhtml);
		    		        byteContent = new byte[(int) inhtml.length()];
		    		        FIN.read(byteContent);
		    		        FIN.close();
		    			}else
		    				byteContent = values[baseCounter+entryC].getBytes(_encoding);
		    			data_raw.write(byteContent);
		    			//data_raw.write(new byte[] {0x0d,0x0a,0});
		    		}
	    			
	    			byte[] data_raw_out = data_raw.toByteArray();
	    			RinfoI.decompressed_size = data_raw_out.length;
	    			
	    			if(grossCompressionType==1) {
						fOutTmp.write(new byte[]{1,0,0,0});
						
		    			int in_len = data_raw_out.length;
						int out_len_preEmpt =  (in_len + in_len / 16 + 64);// + 3
						byte[] record_block_data = new byte[out_len_preEmpt]; 
		    			
						//MInt out_len = new MInt();   //CMN.show(":"+in_len+":"+out_len_preEmpt); 字典太小会抛出
		                //MiniLZO.lzo1x_1_compress(data_raw_out, in_len, record_block_data, out_len, dict);

						
						lzo_uintp out_len = new lzo_uintp();
						new LzoCompressor1x_1().compress(data_raw_out, 0, in_len, record_block_data, 0, out_len);
						//xxx
						//CMN.show(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size)+"asdasd");
						RinfoI.compressed_size = out_len.value;
						fOutTmp.writeInt(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size));
						fOutTmp.write(record_block_data,0,out_len.value);
						fOutTmp.flush();
	    			}else if(grossCompressionType==2) {
						fOutTmp.write(new byte[]{2,0,0,0});
						
						byte[] buffer = new byte[1024];
				    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    	Deflater df = new Deflater();
				    	df.setInput(data_raw_out, 0, (int) RinfoI.decompressed_size);
				    	
				    	df.finish();
				    	//ripemd128.printBytes(raw_data.array(),0, raw_data.position());
				    	//KeyBlockInfoDataLN = df.deflate(KeyBlockInfoData);
				    	while (!df.finished()) {
					    	  int n1 = df.deflate(buffer);
					    	  baos.write(buffer, 0, n1);
				    	}
				    	RinfoI.compressed_size = baos.size();
						fOutTmp.writeInt(BU.calcChecksum(data_raw_out,0,(int) RinfoI.decompressed_size));
				    	fOutTmp.write(baos.toByteArray());
						fOutTmp.flush();
					}
					baseCounter+=blockInfo_L_I;
	    		}
	    		eu_RecordblockInfo.add(RinfoI);
	    	}
	    	int Da_Xiao = fOutTmp.size()-posB;
	    	fOutTmp.close();
	    	
	    	
	    	
	    	fOut.writeLong(blockDataInfo_L.length);
	    	fOut.writeLong(_num_entries);
	    	/*numer of record blocks' info size*/
	    	fOut.writeLong(16*blockDataInfo_L.length);
	    	/*numer of record blocks' size*/
	    	fOut.writeLong(Da_Xiao);
	    	

	    	for(record_info_struct RinfoI:eu_RecordblockInfo) {
		    	fOut.writeLong(RinfoI.compressed_size+8);//INCONGRUNENTSVG unmarked
		    	fOut.writeLong(RinfoI.decompressed_size);//!!!INCONGRUNENTSVG unmarked
	    	}
	    	
	    	tmpIn = new FileInputStream(record_tmp);  
            buf=new byte[1024];  
            n=0;//记录实际读取到的字节数  
            while((n=tmpIn.read(buf))!=-1)  
            {  
                //输出到另一个文件  
            	fOut.write(buf,0,n);  
            }
            fOut.close();
            tmpIn.close();
            index_tmp.delete();
            record_tmp.delete();
            
	    }
	    
	    
	    
	    byte[] KeyBlockInfoData;
	    long KeyBlockInfoDataLN;
	    long KeyBlockInfoDataLN2;
	    int KeyBlockInfoDataDeCompressedAlder32;
	    
	    
	    private void constructKeyBlockInfoData() throws UnsupportedEncodingException {
	    	ByteBuffer raw_data = ByteBuffer.wrap(new byte[_key_block_info_list.length*(8+(65535+2+2)*2+8*2)]);//INCONGRUENTSVG::3 not dyed version diff,interval not marked.
	    	for(key_info_struct infoI:_key_block_info_list) {
	    		raw_data.putLong(infoI.num_entries);
	    		byte[] hTextArray = infoI.headerKeyText;
	    		raw_data.putChar((char) (_encoding.startsWith("UTF-16")?hTextArray.length/2:hTextArray.length));//TODO recollate
	    		raw_data.put(hTextArray);
	    		hTextArray = infoI.tailerKeyText;
		    		if(!_encoding.startsWith("UTF-16")){
		    			raw_data.put(new byte[] {0});
		            }else{        
		            	raw_data.put(new byte[] {0,0});
		            }
	    		raw_data.putChar((char) (_encoding.startsWith("UTF-16")?hTextArray.length/2:hTextArray.length));
	    		raw_data.put(hTextArray);
		    		if(!_encoding.startsWith("UTF-16")){
		    			raw_data.put(new byte[] {0});
		            }else{        
		            	raw_data.put(new byte[] {0,0});
		            }
	    		raw_data.putLong(infoI.key_block_compressed_size+8);//INCONGRUENTSVG this val is faked
	    		raw_data.putLong(infoI.key_block_decompressed_size);
	    	}
	    	KeyBlockInfoDataLN2 = raw_data.position();
	    	
	    	byte[] buffer = new byte[1024];
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	Deflater df = new Deflater();
	    	df.setInput(raw_data.array(), 0, raw_data.position());
	    	KeyBlockInfoDataDeCompressedAlder32 
	    	= BU.calcChecksum(raw_data.array(), 0, raw_data.position());
	    	df.finish();
	    	//ripemd128.printBytes(raw_data.array(),0, raw_data.position());
	    	//KeyBlockInfoDataLN = df.deflate(KeyBlockInfoData);
	    	while (!df.finished()) {
	    	  int n = df.deflate(buffer);
	    	  baos.write(buffer, 0, n);
	    	}
	    	KeyBlockInfoData = baos.toByteArray();
	    	//ripemd128.printBytes(KeyBlockInfoData);
	    	KeyBlockInfoDataLN = KeyBlockInfoData.length;
	    	//ripemd128.printBytes(KeyBlockInfoData,0,(int) KeyBlockInfoDataLN);
	    	//byte[] key_block_info = BU.zlib_decompress(KeyBlockInfoData,0,(int) KeyBlockInfoDataLN);
	    	//ripemd128.printBytes(key_block_info);
	    }
	    
	    
	    
	    
		int perKeyBlockSize_IE_IndexBlockSize = 16;
		int perRecordBlockSize = 32*4;
		long key_block_compressed_size_accumulator;
		long record_block_decompressed_size_accumulator;
	    //int[] dict;
	    int [] offsets;
	    String[] values,keys;
	    Integer[] blockDataInfo_L;
		Integer[] blockInfo_L;
		
		
		
		private void splitKeys() throws IOException {
			final ArrayList<String> keyslist = new ArrayList<String>();
			final ArrayList<String> valslist = new ArrayList<String>();
			data_tree.SetInOrderDo(new inOrderDo() {
				@Override
				public void dothis(RBTNode node) {
					keyslist.add(((myCpr<String,String>)node.getKey()).key);
					valslist.add(((myCpr<String,String>)node.getKey()).value);
			}});
			data_tree.inOrderDo();
			long counter=
					_num_entries=keyslist.size();
			//calc record split
			offsets = new int[(int) _num_entries];
			values = valslist.toArray(new String[] {});
			keys = keyslist.toArray(new String[] {});
			
			ArrayList<Integer> blockInfo = new ArrayList<Integer>();
			ArrayList<Integer> blockDataInfo = new ArrayList<Integer>();
			while(counter>0) {
				int idx = blockInfo.size();
				/*if(blockInfo.size()>0)
					blockInfo.add(blockInfo.get(blockInfo.size()-1));//累积
				else
					blockInfo.add(0);*/
				blockDataInfo.add(0);//byte数量
				blockInfo.add(0);//条目数量

				//if(values[(int) (_num_entries-counter)]==null) {
					//CMN.show("wawa");
					//counter-=1;blockInfo.set(idx, blockInfo.get(idx)+1);//累积
					//break;
				//}
				
				while(true) {
					if(counter<=0) break;
					int recordLen;
					int preJudge;
					if(values[(int) (_num_entries-counter)]!=null) {
						//从内存
						//fetching record data from memory
						byte[] record_data = values[(int) (_num_entries-counter)].getBytes(_encoding);
						recordLen = record_data.length;
						preJudge = blockDataInfo.get(idx)+recordLen;
					}else {
						//fetching record data from file
						File inhtml = fileTree.get(keys[(int) (_num_entries-counter)]);
						recordLen = (int) inhtml.length();
						preJudge = blockDataInfo.get(idx)+recordLen;
					}
					if(preJudge<1024*perRecordBlockSize) {
						//PASSING
						offsets[(int) (_num_entries-counter)] = (int) record_block_decompressed_size_accumulator;//xxx+3*((int) (_num_entries-counter));
						record_block_decompressed_size_accumulator+=recordLen;
						blockDataInfo.set(idx, preJudge);/*offset+=preJudge*/
						blockInfo.set(idx, blockInfo.get(idx)+1);/*entry++*/
						counter-=1;
					}else if(recordLen>=1024*perRecordBlockSize) {
						//MONO OCCUPYING
						offsets[(int) (_num_entries-counter)] = (int) record_block_decompressed_size_accumulator;//xxx+3*((int) (_num_entries-counter));
						record_block_decompressed_size_accumulator+=recordLen;
						blockDataInfo.set(idx, recordLen);/*offset+=preJudge*/  //+3*((int) (_num_entries-counter))
						blockInfo.set(idx, blockInfo.get(idx)+1);/*entry++*/
						counter-=1;
						break;
					}else//NOT PASSING
						break;
				}
			}
			blockDataInfo_L = blockDataInfo.toArray(new Integer[] {});
			blockInfo_L = blockInfo.toArray(new Integer[] {});
			//calc index split
			counter=_num_entries;
			ArrayList<key_info_struct> list = new ArrayList<key_info_struct>();
			key_block_compressed_size_accumulator=0;
			DataOutputStream fOutTmp = new DataOutputStream(new  FileOutputStream(index_tmp));
			while(counter>0) {//总循环
				//dict = new int[102400];//TODO reuse
				ByteBuffer key_block_data_wrap = ByteBuffer.wrap(new byte[1024*perKeyBlockSize_IE_IndexBlockSize]);
				key_info_struct infoI = new key_info_struct();
				long number_entries_counter = 0;
				long baseCounter = _num_entries-counter;
				if(privateZone.containsKey((int) (_num_entries-counter))) {
					int end = privateZone.get((int) (_num_entries-counter));
					for(int i=(int) (_num_entries-counter);i<end;i++) {
						//CMN.show("putting!.."+(_num_entries-counter));
						key_block_data_wrap.putLong(offsets[(int) (_num_entries-counter)]);//占位 offsets i.e. keyid
						key_block_data_wrap.put(keyslist.get((int) (_num_entries-counter)).getBytes(_encoding));
						//CMN.show(number_entries_counter+":"+keyslist.get((int) (_num_entries-counter)));
						if(_encoding.startsWith("UTF-16")){
							key_block_data_wrap.put(new byte[]{0,0});//INCONGRUENTSVG
						}else
							key_block_data_wrap.put(new byte[]{0});//INCONGRUENTSVG
						number_entries_counter++;
						counter--;
					}
					infoI.num_entries = number_entries_counter;
					//CMN.show(baseCounter+":"+number_entries_counter+":"+keyslist.size());
					infoI.headerKeyText = "～～".getBytes(_encoding);
					infoI.tailerKeyText = "～～".getBytes(_encoding);
				}else {
					while(true) {//常规压入entries
						if(counter<=0) break;
						int retPos = key_block_data_wrap.position();
						try {//必定抛出，除非最后一个block.
							if(privateZone.containsKey((int) (_num_entries-counter))) throw new BufferOverflowException();
							key_block_data_wrap.putLong(offsets[(int) (_num_entries-counter)]);//占位 offsets i.e. keyid
							key_block_data_wrap.put(keyslist.get((int) (_num_entries-counter)).getBytes(_encoding));
							//CMN.show(number_entries_counter+":"+keyslist.get((int) (_num_entries-counter)));
							if(_encoding.startsWith("UTF-16")){
								key_block_data_wrap.put(new byte[]{0,0});//INCONGRUENTSVG
							}else
								key_block_data_wrap.put(new byte[]{0});//INCONGRUENTSVG
							counter-=1;
							number_entries_counter+=1;//完整放入后才计数
							//key_block_data.put(new byte[]{0});
						} catch (BufferOverflowException e) {
							//e.printStackTrace();
							key_block_data_wrap.position(retPos);//不完整放入则回退。
							break;
						}
					}
					infoI.num_entries = number_entries_counter;
					//CMN.show(baseCounter+":"+number_entries_counter+":"+keyslist.size());
					infoI.headerKeyText = keyslist.get((int) baseCounter).toLowerCase().replace(" ",emptyStr).replace("-",emptyStr).getBytes(_encoding);
					infoI.tailerKeyText = keyslist.get((int) (baseCounter+number_entries_counter-1)).toLowerCase().replace(" ",emptyStr).replace("-",emptyStr).getBytes(_encoding);
				}
				infoI.key_block_decompressed_size = key_block_data_wrap.position();
				int in_len = (int) infoI.key_block_decompressed_size;
				byte[] key_block_data = key_block_data_wrap.array();
				if(grossCompressionType==1) {//lzo压缩全部
					fOutTmp.write(new byte[]{1,0,0,0});
	    	    	//fOut.write(new byte[] {0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,9,9,9,9,9});
					int out_len_preEmpt =  (in_len + in_len / 16 + 64 );//+ 3
					byte[] compressed_key_block_data = new byte[out_len_preEmpt]; 
					
					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0,(int) infoI.key_block_decompressed_size));
					/*MInt out_len = new MInt();   
					//CMN.show(":"+in_len+":"+out_len_preEmpt); 字典太小会抛出
	                MiniLZO.lzo1x_1_compress(key_block_data, in_len, compressed_key_block_data, out_len, dict);*/

					lzo_uintp out_len = new lzo_uintp();
					new LzoCompressor1x_1().compress(key_block_data, 0, in_len, compressed_key_block_data, 0, out_len);
					
					infoI.key_block_compressed_size = out_len.value;
					fOutTmp.write(compressed_key_block_data,0,out_len.value);
					fOutTmp.flush();
				}else if(grossCompressionType==2) {//zip
					fOutTmp.write(new byte[]{2,0,0,0});
					byte[] buffer = new byte[1024];
			    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    	Deflater df = new Deflater();
			    	df.setInput(key_block_data, 0, in_len);
			    	
			    	df.finish();
			    	//ripemd128.printBytes(raw_data.array(),0, raw_data.position());
			    	//KeyBlockInfoDataLN = df.deflate(KeyBlockInfoData);
			    	
			    	while (!df.finished()) {
				    	  int n1 = df.deflate(buffer);
				    	  baos.write(buffer, 0, n1);
			    	}
			    	infoI.key_block_compressed_size = baos.size();
					fOutTmp.writeInt(BU.calcChecksum(key_block_data,0,in_len));
			    	fOutTmp.write(baos.toByteArray());
					fOutTmp.flush();
				}

				//CMN.show("infoI key_block_data raw");
				//ripemd128.printBytes(key_block_data.array(),0, (int) infoI.key_block_decompressed_size);
				//CMN.show("infoI.key_block_data");
				//ripemd128.printBytes(infoI.key_block_data,0,(int) infoI.key_block_compressed_size);
				//CMN.show(infoI.key_block_decompressed_size+"~~"+infoI.key_block_compressed_size);
				/*
				byte[] key_block_data_return = new byte[(int) infoI.key_block_decompressed_size];
				MInt len = new MInt((int) infoI.key_block_decompressed_size);
				MiniLZO.lzo1x_decompress(infoI.key_block_data,(int) infoI.key_block_compressed_size,key_block_data_return,len);
				CMN.show("key_block_data_return");
				ripemd128.printBytes(key_block_data_return);
				*/
				list.add(infoI);
				key_block_compressed_size_accumulator += infoI.key_block_compressed_size;
			}
			fOutTmp.close();
			//CMN.show("le"+list.size());
			_key_block_info_list = list.toArray(new key_info_struct[] {});
		}

	    public void setZLibCompress(boolean flag) {
	    	if(flag) {
	    		grossCompressionType=2;
	    	}else {
	    		grossCompressionType=1;
	    	}
	    }
	    
	    public void setRecordUnitSize(int val) {
	    	perRecordBlockSize=val;
	    }
	    public void setIndexUnitSize(int val) {
	    	perKeyBlockSize_IE_IndexBlockSize=val;
	    }

		public int getCountOf(String key) {
			return data_tree.getCountOf(new myCpr<>(key,""));
		}

}