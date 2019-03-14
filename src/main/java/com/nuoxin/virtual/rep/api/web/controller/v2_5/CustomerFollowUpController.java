package com.nuoxin.virtual.rep.api.web.controller.v2_5;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nuoxin.virtual.rep.api.web.controller.response.v2_5.set.SearchDynamicFieldListResponseBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nuoxin.virtual.rep.api.common.bean.DefaultResponseBean;
import com.nuoxin.virtual.rep.api.common.util.mem.SessionMemUtils;
import com.nuoxin.virtual.rep.api.entity.DrugUser;
import com.nuoxin.virtual.rep.api.entity.v2_5.CustomerFollowUpPageResponseBean;
import com.nuoxin.virtual.rep.api.service.v2_5.CustomerFollowUpService;
import com.nuoxin.virtual.rep.api.service.v2_5.DrugUserProductService;
import com.nuoxin.virtual.rep.api.utils.CollectionsUtil;
import com.nuoxin.virtual.rep.api.web.controller.request.v2_5.followup.ListRequestBean;
import com.nuoxin.virtual.rep.api.web.controller.request.v2_5.followup.ScreenRequestBean;
import com.nuoxin.virtual.rep.api.web.controller.request.v2_5.followup.SearchRequestBean;
import com.nuoxin.virtual.rep.api.web.controller.response.DrugUserResponseBean;
import com.nuoxin.virtual.rep.api.web.controller.response.product.ProductResponseBean;
import com.nuoxin.virtual.rep.api.web.controller.response.v2_5.CustomerFollowListBean;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import shaded.org.apache.commons.lang3.StringUtils;

/**
 * 客户跟进-首页 Controller 类
 * @author xiekaiyu
 */
@Api(value = "V2.5客户跟进-首页相关接口")
@RequestMapping(value = "/customer/followup/index")
@RestController
public class CustomerFollowUpController extends NewBaseController {

	@Resource
	private SessionMemUtils memUtils;
	@Resource
	private DrugUserProductService drugUserProductService;
	@Resource
	private CustomerFollowUpService customerFollowService;

	@Value("${download.file.path}")
	private String downloadPath;


	@SuppressWarnings("unchecked")
	@ApiOperation(value = "高级搜索用到的筛选条件")
	@RequestMapping(value = "/search/dynamic/field/list/{productId}", method = { RequestMethod.GET })
	public DefaultResponseBean<SearchDynamicFieldListResponseBean> getSearchDynamicFieldList(HttpServletRequest request,
																		@PathVariable(value = "productId") Long productId) {
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}

		SearchDynamicFieldListResponseBean searchDynamicField = customerFollowService.getSearchDynamicField(productId);
		DefaultResponseBean<SearchDynamicFieldListResponseBean> responseBean = new DefaultResponseBean<>();
		responseBean.setData(searchDynamicField);

		return responseBean;
	}


	@SuppressWarnings("unchecked")
	@ApiOperation(value = "客户医生拜访列表信息")
	@RequestMapping(value = "/list", method = { RequestMethod.POST })
	public DefaultResponseBean<CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>>> list(HttpServletRequest request,
			@RequestBody SearchRequestBean indexRequest) {
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}

		// 从会话变量中获取 leaderPath
		String leaderPath = user.getLeaderPath();
		indexRequest.setDrugUserId(user.getId());
		CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>> pageResponse = customerFollowService.list(indexRequest,
				leaderPath);
		DefaultResponseBean<CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>>> responseBean = new DefaultResponseBean<>();
		responseBean.setData(pageResponse);

		return responseBean;
	}

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "搜索接口")
	@RequestMapping(value = "/search", method = { RequestMethod.POST })
	public DefaultResponseBean<CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>>> search(HttpServletRequest request,
			@RequestBody SearchRequestBean searchRequest) {
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}

		String search = searchRequest.getSearch();
		if (StringUtils.isBlank(search)) {
			return super.getParamsErrorResponse("search is blank");
		}

		List<Long> virtualDrugUserIds = null;
		List<DrugUserResponseBean> subordinates = drugUserProductService.getSubordinates(user.getLeaderPath());
		if (CollectionsUtil.isNotEmptyList(subordinates)) {
			List<Long> virtualDrugUserIdsTemp = new ArrayList<>();
			subordinates.forEach(subordinate -> {
				virtualDrugUserIdsTemp.add(subordinate.getId());
			});
			virtualDrugUserIds = virtualDrugUserIdsTemp;
			searchRequest.setVirtualDrugUserIds(virtualDrugUserIds);
		}

//		List<Long> productLineIds = null;
//		List<ProductResponseBean> products = drugUserProductService.getProductsByDrugUserId(user.getLeaderPath());
//		if (CollectionsUtil.isNotEmptyList(products)) {
//			List<Long> productLineIdsTemp = new ArrayList<>();
//			products.forEach(product ->{
//				productLineIdsTemp.add(product.getProductId());
//			});
//			productLineIds = productLineIdsTemp;
//			searchRequest.setProductLineIds(productLineIds);
//		}

		searchRequest.setDrugUserId(user.getId());
		CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>> pageResponse = customerFollowService.search(searchRequest,
				user.getLeaderPath());
		DefaultResponseBean<CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>>> responseBean = new DefaultResponseBean<>();
		responseBean.setData(pageResponse);
		return responseBean;
	}

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "筛选接口")
	@RequestMapping(value = "/screen", method = { RequestMethod.POST })
	public DefaultResponseBean<CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>>> screen(
			@RequestBody SearchRequestBean screenRequest, HttpServletRequest request) {
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}

		List<Long> virtualDrugUserIds = screenRequest.getVirtualDrugUserIds();
		if (CollectionsUtil.isEmptyList(virtualDrugUserIds)) {
			return super.getParamsErrorResponse("virtualDrugUserIds is empty list");
		}
		
		if (virtualDrugUserIds.size() == 1 && virtualDrugUserIds.get(0) == -1) {
			List<DrugUserResponseBean> subordinates = drugUserProductService.getSubordinates(user.getLeaderPath());
			if (CollectionsUtil.isNotEmptyList(subordinates)) {
				List<Long> virtualDrugUserIdsTemp = new ArrayList<>();
				subordinates.forEach(subordinate -> {
					virtualDrugUserIdsTemp.add(subordinate.getId());
				});
				virtualDrugUserIds = virtualDrugUserIdsTemp;
				screenRequest.setVirtualDrugUserIds(virtualDrugUserIds);
			}
		}

		List<Long> productLineIds = screenRequest.getProductLineIds();
		if (CollectionsUtil.isEmptyList(productLineIds)) {
			return super.getParamsErrorResponse("productLineIds is empty list");
		}
		
		if (productLineIds.size() == 1 && productLineIds.get(0) == -1) {
			List<ProductResponseBean> products = drugUserProductService.getProductsByDrugUserId(user.getLeaderPath());
			if (CollectionsUtil.isNotEmptyList(products)) {
				List<Long> productLineIdsTemp = new ArrayList<>();
				products.forEach(product ->{
					productLineIdsTemp.add(product.getProductId());
				});
				productLineIds = productLineIdsTemp;
				screenRequest.setProductLineIds(productLineIds);
			}
		}

		screenRequest.setDrugUserId(user.getId());
		CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>> pageResponse = customerFollowService.screen(screenRequest);
		DefaultResponseBean<CustomerFollowUpPageResponseBean<List<CustomerFollowListBean>>> responseBean = new DefaultResponseBean<>();
		responseBean.setData(pageResponse);

		return responseBean;
	}

	@ApiOperation(value = "更多筛选接口 @田存补充")
	@RequestMapping(value = "/screen/more", method = { RequestMethod.POST })
	public ResponseEntity<DefaultResponseBean<Boolean>> searchMore(@RequestBody Object object) {
		// TODO @田存
		return null;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "获取所有下属医药代表信息(医药代表)")
	@RequestMapping(value = "/drug_users/get", method = { RequestMethod.GET })
	public DefaultResponseBean<List<DrugUserResponseBean>> getSubordinates(HttpServletRequest request) {
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}

		List<DrugUserResponseBean> list = drugUserProductService.getSubordinates(user.getLeaderPath());
		DefaultResponseBean<List<DrugUserResponseBean>> responseBean = new DefaultResponseBean<>();
		responseBean.setData(list);

		return responseBean;
	}

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "根据获取所有产品线信息(产品)")
	@RequestMapping(value = "/product_lines/get", method = { RequestMethod.GET })
	public DefaultResponseBean<List<ProductResponseBean>> getAllProductLines(HttpServletRequest request) {
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}

		List<ProductResponseBean> list = drugUserProductService.getProductsByDrugUserId(user.getLeaderPath());
		DefaultResponseBean<List<ProductResponseBean>> responseBean = new DefaultResponseBean<>();
		responseBean.setData(list);

		return responseBean;
	}

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "获得下载医生excel名称")
	@RequestMapping(value = "/exportExcelAndGetName", method = { RequestMethod.POST })
	public DefaultResponseBean<Map> exportExcel(HttpServletRequest request, @RequestBody SearchRequestBean searchReq){
		DrugUser user = super.getDrugUser(request);
		if (user == null) {
			return super.getLoginErrorResponse();
		}
		searchReq.setDrugUserId(user.getId());

		List<Long> virtualDrugUserIds = searchReq.getVirtualDrugUserIds();
		if (CollectionsUtil.isEmptyList(virtualDrugUserIds)) {
			return super.getParamsErrorResponse("virtualDrugUserIds is empty list");
		}

		List<Long> productLineIds = searchReq.getProductLineIds();
		if (CollectionsUtil.isEmptyList(productLineIds)) {
			return super.getParamsErrorResponse("productLineIds is empty list");
		}

		Map<String, String> map = new HashMap<String, String>();
		DefaultResponseBean<Map> responseBean = new DefaultResponseBean<>();
		try{
			String excelName = customerFollowService.getExportExcelName(searchReq);
			map.put("filename", excelName);
		}catch (Exception e){
			e.printStackTrace();
			return super.getParamsErrorResponse("错误");
		}
		responseBean.setData(map);
		return responseBean;
	}

	@ApiOperation(value = "下载医生详细信息", notes = "下载医生详细信息")
	@GetMapping("/downloadExcel")
	public void downloadExcel(@RequestParam("filename")String filename, HttpServletResponse response) {
		try {
			response.setContentType("application/vnd.ms-excel");
			//2)、设置Content-Disposition
			response.setHeader("Content-Disposition", "attachment;filename="+filename);
			//3)、输出流
			OutputStream out = response.getOutputStream();
			InputStream in = new FileInputStream(new File(downloadPath + filename));
			int b;
			while((b=in.read())!=-1){
				out.write(b);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
