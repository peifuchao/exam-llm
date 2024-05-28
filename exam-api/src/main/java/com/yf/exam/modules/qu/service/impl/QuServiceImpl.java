package com.yf.exam.modules.qu.service.impl;

import cn.hutool.poi.excel.cell.CellSetter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yf.exam.core.api.dto.PagingReqDTO;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.llm.ChatAgent;
import com.yf.exam.modules.qu.dto.QuAnswerDTO;
import com.yf.exam.modules.qu.dto.QuDTO;
import com.yf.exam.modules.qu.dto.export.QuExportDTO;
import com.yf.exam.modules.qu.dto.ext.QuDetailDTO;
import com.yf.exam.modules.qu.dto.request.QuQueryReqDTO;
import com.yf.exam.modules.qu.entity.GQuestion;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.entity.QuAnswer;
import com.yf.exam.modules.qu.entity.QuRepo;
import com.yf.exam.modules.qu.enums.QuType;
import com.yf.exam.modules.qu.mapper.QuMapper;
import com.yf.exam.modules.qu.service.QuAnswerService;
import com.yf.exam.modules.qu.service.QuRepoService;
import com.yf.exam.modules.qu.service.QuService;
import com.yf.exam.modules.repo.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * <p>
 * 语言设置 服务实现类
 * </p>
 *
 * @author 聪明笨狗
 * @since 2020-05-25 10:17
 */
@Service
public class QuServiceImpl extends ServiceImpl<QuMapper, Qu> implements QuService {

    @Autowired
    private QuAnswerService quAnswerService;

    @Autowired
    private QuRepoService quRepoService;

    @Autowired
    private RepoService repoService;

    @Autowired
    private ChatAgent chatAgent;

    @Override
    public IPage<QuDTO> paging(PagingReqDTO<QuQueryReqDTO> reqDTO) {

        //创建分页对象
        Page page = new Page<>(reqDTO.getCurrent(), reqDTO.getSize());

        //转换结果
        IPage<QuDTO> pageData = baseMapper.paging(page, reqDTO.getParams());
        return pageData;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<String> ids) {
        // 移除题目
        this.removeByIds(ids);

        // 移除选项
        QueryWrapper<QuAnswer> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(QuAnswer::getQuId, ids);
        quAnswerService.remove(wrapper);

        // 移除题库绑定
        QueryWrapper<QuRepo> wrapper1 = new QueryWrapper<>();
        wrapper1.lambda().in(QuRepo::getQuId, ids);
        quRepoService.remove(wrapper1);
    }

    @Override
    public List<Qu> listByRandom(String repoId, Integer quType, List<String> excludes, Integer size) {
        return baseMapper.listByRandom(repoId, quType, excludes, size);
    }

    @Override
    public QuDetailDTO detail(String id) {

        QuDetailDTO respDTO = new QuDetailDTO();
        Qu qu = this.getById(id);
        BeanMapper.copy(qu, respDTO);

        List<QuAnswerDTO> answerList = quAnswerService.listByQu(id);
        respDTO.setAnswerList(answerList);

        List<String> repoIds = quRepoService.listByQu(id);
        respDTO.setRepoIds(repoIds);

        return respDTO;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void save(QuDetailDTO reqDTO) {


        // 校验数据
        this.checkData(reqDTO, "");

        Qu qu = new Qu();
        BeanMapper.copy(reqDTO, qu);


        // 更新
        this.saveOrUpdate(qu);

        // 保存全部问题
        quAnswerService.saveAll(qu.getId(), reqDTO.getAnswerList());

        // 保存到题库
        quRepoService.saveAll(qu.getId(), qu.getQuType(), reqDTO.getRepoIds());

    }

    @Override
    public List<QuExportDTO> listForExport(QuQueryReqDTO query) {
        return baseMapper.listForExport(query);
    }

    @Override
    public int importExcel(List<QuExportDTO> dtoList) {

        //根据题目名称分组
        Map<Integer, List<QuExportDTO>> anMap = new HashMap<>(16);

        //题目本体信息
        Map<Integer, QuExportDTO> quMap = new HashMap<>(16);

        //数据分组
        for (QuExportDTO item : dtoList) {

            // 空白的ID
            if (StringUtils.isEmpty(item.getNo())) {
                continue;
            }

            Integer key;
            //序号
            try {
                key = Integer.parseInt(item.getNo());
            } catch (Exception e) {
                continue;
            }

            //如果已经有题目了，直接处理选项
            if (anMap.containsKey(key)) {
                anMap.get(key).add(item);
            } else {
                //如果没有，将题目内容和选项一起
                List<QuExportDTO> subList = new ArrayList<>();
                subList.add(item);
                anMap.put(key, subList);
                quMap.put(key, item);
            }
        }

        int count = 0;
        try {

            //循环题目插入
            for (Integer key : quMap.keySet()) {

                QuExportDTO im = quMap.get(key);

                //题目基本信息
                QuDetailDTO qu = new QuDetailDTO();
                qu.setContent(im.getQContent());
                qu.setAnalysis(im.getQAnalysis());
                qu.setQuType(Integer.parseInt(im.getQuType()));
                qu.setCreateTime(new Date());

                //设置回答列表
                List<QuAnswerDTO> answerList = this.processAnswerList(anMap.get(key));
                //设置题目
                qu.setAnswerList(answerList);
                //设置引用题库
                qu.setRepoIds(im.getRepoList());
                // 保存答案
                this.save(qu);
                count++;
            }

        } catch (ServiceException e) {
            e.printStackTrace();
            throw new ServiceException(1, "导入出现问题，行：" + count + "，" + e.getMessage());
        }

        return count;
    }

    @Override
    public String generateAnalysis(String id) {
        Qu qu = this.getById(id);



        List<QuAnswerDTO> answers = quAnswerService.listByQu(qu.getId());


        String message=this.formatMessage(qu, answers);

        System.out.println(message);

        return chatAgent.chat(message);
    }

    private String formatMessage(Qu qu, List<QuAnswerDTO> answers) {

        StringBuilder sb = new StringBuilder();
        sb.append("生成解析：\n");
        if(qu.getQuType()==1)
            sb.append("单选题:");
        else
            sb.append("多选题：");
        sb.append("\n");
        sb.append("题目：").append(qu.getContent()).append("\n");
        sb.append("选项：").append("\n");
        for (QuAnswerDTO a : answers) {
            sb.append(a.getContent()).append("\n");
        }
        sb.append("正确答案：");
        for (QuAnswerDTO a : answers) {
            if (a.getIsRight()) {
                sb.append(a.getContent()).append(" ");
            }
        }
        return sb.toString();

    }

    /**
     * 处理回答列表
     *
     * @param importList
     * @return
     */
    private List<QuAnswerDTO> processAnswerList(List<QuExportDTO> importList) {

        List<QuAnswerDTO> list = new ArrayList<>(16);
        for (QuExportDTO item : importList) {
            QuAnswerDTO a = new QuAnswerDTO();
            a.setIsRight("1".equals(item.getAIsRight()));
            a.setContent(item.getAContent());
            a.setAnalysis(item.getAAnalysis());
            a.setId("");
            list.add(a);
        }
        return list;
    }

    /**
     * 校验题目信息
     *
     * @param qu
     * @param no
     * @throws Exception
     */
    public void checkData(QuDetailDTO qu, String no) {


        if (StringUtils.isEmpty(qu.getContent())) {
            throw new ServiceException(1, no + "题目内容不能为空！");
        }


        if (CollectionUtils.isEmpty(qu.getRepoIds())) {
            throw new ServiceException(1, no + "至少要选择一个题库！");
        }

        List<QuAnswerDTO> answers = qu.getAnswerList();


        if (CollectionUtils.isEmpty(answers)) {
            throw new ServiceException(1, no + "客观题至少要包含一个备选答案！");
        }


        int trueCount = 0;
        for (QuAnswerDTO a : answers) {

            if (a.getIsRight() == null) {
                throw new ServiceException(1, no + "必须定义选项是否正确项！");
            }

            if (StringUtils.isEmpty(a.getContent())) {
                throw new ServiceException(1, no + "选项内容不为空！");
            }

            if (a.getIsRight()) {
                trueCount += 1;
            }
        }

        if (trueCount == 0) {
            throw new ServiceException(1, no + "至少要包含一个正确项！");
        }


        //单选题
        if (qu.getQuType().equals(QuType.RADIO) && trueCount > 1) {
            throw new ServiceException(1, no + "单选题不能包含多个正确项！");
        }

    }

    /**
     * 生成题目
     *
     */
    public int generateQuestion(String type,String subject, String level, int num,String[] repoIds){

        String message = formatQuestionMsg(type,subject,level,num);

        String json = chatAgent.chat(message);



        json = json.substring(1,json.length()-1);
        json = json.replace("\"questions\": ","");

        ObjectMapper mapper = new ObjectMapper();
        List<GQuestion> questions=null;
        try {
            questions = mapper.readValue(json, new TypeReference<List<GQuestion>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(questions==null) return 0;
        List<String> id= Arrays.asList(repoIds);


        for(GQuestion q:questions){
            QuDetailDTO qu = new QuDetailDTO();
            qu.setContent(q.getQuestion());
            qu.setCreateTime(new Date());
            qu.setLevel(Integer.parseInt(level));
            qu.setQuType(Integer.parseInt(type));
            qu.setRepoIds(id);
            List<QuAnswerDTO> answerList = getAnswerList(q.getAnswer(),q.getOptions());
            qu.setAnswerList(answerList);

            this.save(qu);
        }
        return questions.size();
    }

    private List<QuAnswerDTO> getAnswerList(String answer, String options) {
        List<QuAnswerDTO> answerDTOS = new ArrayList<>();
        String[] option = options.split(";");
        String[] b = {"A.","B.","C.","D."};
        if(options.contains("A.")){
            for(int i=0;i<4;i++){
                QuAnswerDTO answerDTO = new QuAnswerDTO();
                answerDTO.setContent(option[i].replace(b[i],"").replace(" ",""));
                answerDTO.setIsRight(answer.contains(b[i].substring(0, 1)));
                answerDTOS.add(answerDTO);
            }
        }else{
            for(String o : option){
                QuAnswerDTO answerDTO = new QuAnswerDTO();
                answerDTO.setContent(o);
                answerDTO.setIsRight(answer.contains(o));
                answerDTOS.add(answerDTO);
            }
        }

        return answerDTOS;

    }


    private String formatQuestionMsg(String type, String subject, String level, int num) {

        StringBuilder sb = new StringBuilder();
        sb.append("生成题目：\n");

        sb.append("题目类型：");
        if(type.equals("1"))
            sb.append("单选题:");
        else if(type.equals("2"))
            sb.append("多选题：");
        else
            sb.append("判断题：");
        sb.append("\n");

        sb.append("科目：").append(subject).append("\n");


        sb.append("难度：");
        if(level.equals("1"))
            sb.append("普通");
        else
            sb.append("较难");
       sb.append("\n");
        sb.append("数量：").append(num).append("\n");
        return sb.toString();
    }


}
