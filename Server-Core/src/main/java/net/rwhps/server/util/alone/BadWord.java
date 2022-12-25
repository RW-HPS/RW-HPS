/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.alone;

import net.rwhps.server.util.io.IoReadConversion;

import java.io.BufferedReader;
import java.util.*;

/**
 * 过滤 屏蔽词
 *
 * @author yqwang0907
 * @author RW-HPS/Dr
 */
public class BadWord {
    public String filePath = "/other/Sensitive_Thesaurus.txt";
    public Set<String> words;
    public Map<String,String> wordMap;
    public int minMatchTYpe = 1;
    public int maxMatchType = 2;

    public BadWord() {
        words = readTxtByLine(filePath);
        addBadWordToHashMap(words);
    }

    private Set<String> readTxtByLine(String path){
        Set<String> keyWordSet = new HashSet<String>();
        BufferedReader reader=null;
        String temp;
        try{
            reader= IoReadConversion.streamBufferRead(Objects.requireNonNull(BadWord.class.getResourceAsStream(path)));
            while((temp=reader.readLine())!=null){
                keyWordSet.add(temp);
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            if(reader!=null){
                try{
                    reader.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        return keyWordSet;
    }
    /**
     * 检查文字中是否包含敏感字符，检查规则如下：<br>
     * @param txt
     * @param beginIndex
     * @param matchType
     * @return 如果存在，则返回敏感词字符的长度，不存在返回0
     */
    @SuppressWarnings({ "rawtypes"})
    public int checkBadWord(String txt,int beginIndex,int matchType){
        boolean  flag = false;
        int matchFlag = 0;
        char word = 0;
        Map nowMap = wordMap;
        for(int i = beginIndex; i < txt.length() ; i++){
            word = txt.charAt(i);
            nowMap = (Map) nowMap.get(word);
            if(nowMap != null){
                matchFlag++;
                if("1".equals(nowMap.get("isEnd"))){
                    flag = true;
                    if(minMatchTYpe == matchType){
                        break;
                    }
                }
            } else {
                break;
            }
        }
        if(!flag){
            matchFlag = 0;
        }
        return matchFlag;
    }

    /**
     * 替换敏感字字符
     * @param txt
     * @param matchType
     * @param replaceChar 替换字符，默认*
     */
    public String replaceBadWord(String txt,int matchType,String replaceChar){
        String resultTxt = txt;
        Set<String> set = getBadWord(txt, matchType);
        Iterator<String> iterator = set.iterator();
        String word = null;
        String replaceString = null;
        while (iterator.hasNext()) {
            word = iterator.next();
            replaceString = getReplaceChars(replaceChar, word.length());
            resultTxt = resultTxt.replaceAll(word, replaceString);
        }

        return resultTxt;
    }
    /**
     * 获取文字中的敏感词
     * @param txt 文字
     * @param matchType 匹配规则 1：最小匹配规则，2：最大匹配规则
     * @return
     */
    public Set<String> getBadWord(String txt , int matchType){
        Set<String> sensitiveWordList = new HashSet<String>();

        for(int i = 0 ; i < txt.length() ; i++){
            int length = checkBadWord(txt, i, matchType);
            if(length > 0){
                sensitiveWordList.add(txt.substring(i, i+length));
                i = i + length - 1;
            }
        }

        return sensitiveWordList;
    }

    /**
     * 获取替换字符串
     * @param replaceChar
     * @param length
     * @return
     */
    private String getReplaceChars(String replaceChar,int length){
        String resultReplace = replaceChar;
        for(int i = 1 ; i < length ; i++){
            resultReplace += replaceChar;
        }

        return resultReplace;
    }

    /**
     * 将我们的敏感词库构建成了一个类似与一颗一颗的树，这样我们判断一个词是否为敏感词时就大大减少了检索的匹配范围。
     * @param keyWordSet 敏感词库
     * @author yqwang0907
     * @date 2018年2月28日下午5:28:08
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addBadWordToHashMap(Set<String> keyWordSet) {
        //初始化敏感词容器，减少扩容操作
        wordMap = new HashMap(keyWordSet.size());
        String key = null;
        Map nowMap = null;
        Map<String, String> newWorMap = null;
        //迭代keyWordSet
        Iterator<String> iterator = keyWordSet.iterator();
        while(iterator.hasNext()){
            key = iterator.next();
            nowMap = wordMap;
            for(int i = 0 ; i < key.length() ; i++){
                char keyChar = key.charAt(i);
                Object wordMap = nowMap.get(keyChar);
                if(wordMap != null){
                    nowMap = (Map) wordMap;
                }
                else{
                    newWorMap = new HashMap<String,String>(256);
                    newWorMap.put("isEnd", "0");
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap;
                }

                if(i == key.length() - 1) {
                    nowMap.put("isEnd", "1");
                }
            }
        }
    }

    public Set<String> badWord(String text) {
        return getBadWord(text, 2);
    }
}