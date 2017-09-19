package com.nuoxin.virtual.rep.api.service;

import com.nuoxin.virtual.rep.api.common.bean.PageResponseBean;
import com.nuoxin.virtual.rep.api.common.service.BaseService;
import com.nuoxin.virtual.rep.api.common.util.StringUtils;
import com.nuoxin.virtual.rep.api.dao.DoctorRepository;
import com.nuoxin.virtual.rep.api.entity.Doctor;
import com.nuoxin.virtual.rep.api.web.controller.request.QueryRequestBean;
import com.nuoxin.virtual.rep.api.web.controller.request.doctor.DoctorRequestBean;
import com.nuoxin.virtual.rep.api.web.controller.response.doctor.DoctorResponseBean;
import com.nuoxin.virtual.rep.api.web.controller.response.doctor.DoctorStatResponseBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by fenggang on 9/11/17.
 */
@Service
@Transactional(readOnly = true)
public class DoctorService extends BaseService {

    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private DrugUserService drugUserService;

    public Doctor findById(Long id){
        return doctorRepository.findOne(id);
    }

    public Doctor findByMobile(String mobile){
        return doctorRepository.findTopByMobile(mobile);
    }

    public PageResponseBean<DoctorResponseBean> page(QueryRequestBean bean){
        PageRequest pageable = super.getPage(bean);
        Specification<Doctor> spec = new Specification<Doctor>() {
            @Override
            public Predicate toPredicate(Root<Doctor> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if(bean.getDrugUserId()!=null && bean.getDrugUserId()!=0){
                    predicates.add(cb.like(root.get("drugUserIds").as(String.class),"%"+bean.getDrugUserId()+",%"));
                }
                if(StringUtils.isNotEmtity(bean.getName())){
                    predicates.add(cb.like(root.get("name").as(String.class),"%"+bean.getName()+"%"));
                }
                if(StringUtils.isNotEmtity(bean.getMobile())){
                    predicates.add(cb.like(root.get("mobile").as(String.class),"%"+bean.getMobile()+"%"));
                }
                if(StringUtils.isNotEmtity(bean.getDepartment())){
                    predicates.add(cb.like(root.get("department").as(String.class),"%"+bean.getDepartment()+"%"));
                }
                if(StringUtils.isNotEmtity(bean.getDoctorLevel())){
                    predicates.add(cb.like(root.get("doctorLevel").as(String.class),"%"+bean.getDoctorLevel()+"%"));
                }
                if(StringUtils.isNotEmtity(bean.getHospital())){
                    predicates.add(cb.like(root.get("hospitalName").as(String.class),"%"+bean.getHospital()+"%"));
                }
                query.where(cb.and(cb.and(predicates.toArray(new Predicate[0]))));
                return query.getRestriction();
            }
        };
        Page<Doctor> page = doctorRepository.findAll(spec,pageable);
        PageResponseBean<DoctorResponseBean> responseBean = new PageResponseBean<>(page);
        List<Doctor> pageList = page.getContent();
        if(pageList!=null && !pageList.isEmpty()){
            List<DoctorResponseBean> list = new ArrayList<>();
            for (Doctor doctor:pageList) {
                DoctorResponseBean listBean = new DoctorResponseBean();
                BeanUtils.copyProperties(doctor,listBean);
                listBean.setDoctorId(doctor.getId());
                listBean.setDoctorMobile(doctor.getMobile());
                listBean.setDoctorName(doctor.getName());
                list.add(listBean);
            }
            responseBean.setContent(list);
        }
        return responseBean;
    }

    public DoctorStatResponseBean stat(Long drugUserId){
        DoctorStatResponseBean responseBean = new DoctorStatResponseBean();
        Map<String,Long> map = doctorRepository.statDrugUserDoctorNum("%"+drugUserId+",%");
        if(map!=null){
            responseBean.setDoctorNum(map.get("doctorNum")!=null?Integer.valueOf(map.get("doctorNum")+""):0);
            responseBean.setHospitalNum(map.get("hospitalNum")!=null?Integer.valueOf(map.get("hospitalNum")+""):0);
        }
        return responseBean;
    }

    @Transactional(readOnly = false)
    public Boolean save(DoctorRequestBean bean){
        Doctor doctor = doctorRepository.findTopByMobile(bean.getMobile());
        if(doctor==null){
            doctor = new Doctor();
            doctor.setDrugUserIds(bean.getDrugUserId()+",");
        }else{
            doctor.setDrugUserIds(doctor.getDrugUserIds()+bean.getDrugUserId()+',');
        }
//        BeanUtils.copyProperties(bean,doctor);
        doctor.setCity(bean.getCity());
        doctor.setClientLevel(bean.getClientLevel());
        doctor.setDepartment(bean.getDepartment());
        doctor.setDoctorLevel(bean.getDoctorLevel());
        doctor.setHospitalLevel(bean.getHospitalLevel());
        doctor.setHospitalName(bean.getHospitalName());
        doctor.setMobile(bean.getMobile());
        doctor.setName(bean.getName());
        //TODO  获取主数据id

        doctor = doctorRepository.saveAndFlush(doctor);
        if(doctor.getId()!=null){
            return true;
        }
        return false;
    }

    @Transactional(readOnly = false)
    public Boolean saves(List<Doctor> list){
        return true;
    }
}