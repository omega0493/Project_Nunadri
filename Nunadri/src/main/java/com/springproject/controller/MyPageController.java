package com.springproject.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springproject.common.FileUtils;
import com.springproject.impl.UserDetailsServiceImpl;
import com.springproject.service.CommunityService;
import com.springproject.service.MemberService;
import com.springproject.service.MyhouseService;
import com.springproject.vo.CommunityVO;
import com.springproject.vo.Criteria;
import com.springproject.vo.MemberVO;
import com.springproject.vo.NoticeMyhouseVO;
import com.springproject.vo.PageVO;
import com.springproject.vo.SecurityUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MyPageController {

	private final MemberService memberservice;

	private final PasswordEncoder encoder;

	private final MyhouseService myhouseService;

	private final UserDetailsServiceImpl UserDetailsServiceImpl;

	private final CommunityService communityService;

	static String condition = "";
	static String keyword = "";

	// 마이페이지 메인
	@GetMapping("/mypage")
	public String myPageMain(@AuthenticationPrincipal SecurityUser user, Model model) {
		MemberVO member = memberservice.getMemberInfo(user.getId());
		model.addAttribute("memberInfo", member);
		return "view/member/mypage/mypage_main";
	}

	// 회원정보 수정 페이지
	@GetMapping("/mypagemodify")
	public String myPage(@AuthenticationPrincipal SecurityUser user, Model model) {
		MemberVO member = memberservice.getMemberInfo(user.getId());
		model.addAttribute("memberInfo", member);
		return "view/member/mypage/member_modify";
	}

	// 비밀번호 변경 페이지
	@GetMapping("/changePassword")
	public String changePassword() {
		return "view/member/mypage/change_pw";
	}

	// 게시판(공지사항, 도와주세요, 자유게시판) 목록
	@GetMapping("/myhouseBoard/{category}") //
	public String noticeBoard(@PathVariable("category") String category, NoticeMyhouseVO myhouseBoardList,
			@AuthenticationPrincipal SecurityUser user, Model model, Criteria cri) {

		myhouseBoardList.setMyhouseCategory(category);
		//
		myhouseBoardList.setHouseNo(myhouseService.getHouseNo(user.getNickname()));

		myhouseBoardList.setNickname(user.getNickname());

		myhouseBoardList.setId(user.getId());

		System.out.println(user.getId());

		// 검색값 없을때 기본 값 설정
		if (myhouseBoardList.getSearchCondition() == null) {
			myhouseBoardList.setSearchCondition("MYHOUSE_TITLE");
		}
		if (myhouseBoardList.getSearchKeyword() == null) {
			myhouseBoardList.setSearchKeyword("");
		}

		// 검색, 키워드 값(페이징 처리시 필요)
		condition = myhouseBoardList.getSearchCondition();
		keyword = myhouseBoardList.getSearchKeyword();

		int total = myhouseService.selectMyHouseBoardCount(myhouseBoardList);

		model.addAttribute("category", category);
		model.addAttribute("paramList", myhouseService.memberMyhouseBoardList(myhouseBoardList, cri));
		model.addAttribute("pageMaker", new PageVO(cri, total));
		model.addAttribute("condition", myhouseBoardList.getSearchCondition());
		model.addAttribute("keyword", myhouseBoardList.getSearchKeyword());

		System.out.println(myhouseService.memberMyhouseBoardList(myhouseBoardList, cri));

		return "view/member/mypage/member_myhouse_boarder_list";
	}

	@RequestMapping(value = "/myhouseBoard/{category}") //
	@ResponseBody
	public String noticeBoard1(@RequestParam Map<String, Object> parameters, @PathVariable("category") String category,
			NoticeMyhouseVO myhouseBoardList, @AuthenticationPrincipal SecurityUser user, Model model, Criteria cri)
			throws JsonMappingException, JsonProcessingException {

		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> hashMap = new HashMap<String, Object>();

		myhouseBoardList.setMyhouseCategory(category);
		//
		myhouseBoardList.setHouseNo(myhouseService.getHouseNo(user.getNickname()));

		myhouseBoardList.setNickname(user.getNickname());

		myhouseBoardList.setId(user.getId());

		// 검색값 없을때 기본 값 설정
		if (myhouseBoardList.getSearchCondition() == null) {
			myhouseBoardList.setSearchCondition("MYHOUSE_TITLE");
		}
		if (myhouseBoardList.getSearchKeyword() == null) {
			myhouseBoardList.setSearchKeyword("");
		}

		// 검색, 키워드 값(페이징 처리시 필요)
		condition = myhouseBoardList.getSearchCondition();
		keyword = myhouseBoardList.getSearchKeyword();

		int total = myhouseService.selectMyHouseBoardCount(myhouseBoardList);

		model.addAttribute("category", category);
		model.addAttribute("boardList", myhouseService.memberMyhouseBoardList(myhouseBoardList, cri));
		model.addAttribute("pageMaker", new PageVO(cri, total));
		model.addAttribute("condition", myhouseBoardList.getSearchCondition());
		model.addAttribute("keyword", myhouseBoardList.getSearchKeyword());

		List<NoticeMyhouseVO> paramList = myhouseService.memberMyhouseBoardList(myhouseBoardList, cri);
		hashMap.put("paramList", paramList);
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(hashMap);

		return json;
	}

	// 회원정보 수정
	@PostMapping(value = "/update")
	@ResponseBody
	public String updateMember(@AuthenticationPrincipal SecurityUser user, MemberVO vo, HttpSession session) {

		if (memberservice.findEmail(vo.getEmail()) != null
				&& !vo.getEmail().equals(memberservice.findId(user.getId()).getEmail())) {
			throw new IllegalStateException("이미 가입된 이메일입니다.");
		} else if (memberservice.findNickname(vo.getNickname()) != null
				&& !vo.getNickname().equals(user.getNickname())) {

			throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
		} else {
			// 회원 정보 업데이트
			memberservice.updateMember(vo);

			// user에 직접 들어갈 수 있도록 여기서 데이터 입력해줌
			UserDetails userDetails = UserDetailsServiceImpl.loadUserByUsername(vo.getId());

			// 세션 등록
			Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
					userDetails.getAuthorities());
			SecurityContext securityContext = SecurityContextHolder.getContext();
			securityContext.setAuthentication(authentication);

			session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

			return "/mypage";
		}

	}

	// 회원 탈퇴
	@GetMapping("/deleteMember")
	public String deleteMember(@AuthenticationPrincipal SecurityUser user) {

		memberservice.deleteMember(user.getId());

		// 세션 종료 후 로그아웃
		SecurityContextHolder.clearContext();

		return "redirect:/";
	}

	// 회원 탈퇴
	@GetMapping("/deleteAdmin/{id}")
	public String deleteAdmin(@PathVariable("id") String id) {

		memberservice.deleteMember(id);

		return "redirect:/admin";
	}

	// 비밀번호 변경
	@PostMapping("/changePassword")
	@ResponseBody
	public String checkPassword(@AuthenticationPrincipal SecurityUser user, String pwd) {

		MemberVO member = new MemberVO();

		member.setId(user.getId());
		pwd = encoder.encode(pwd);
		member.setPwd(pwd);

		memberservice.updatePwd(member);

		return "/mypage";
	}

	@RequestMapping("/admin")
	public String admin(@AuthenticationPrincipal SecurityUser user, Model model, MemberVO vo, Criteria cri) {

		// 검색값 없을때 기본 값 설정
		if (vo.getSearchCondition() == null) {
			vo.setSearchCondition("ID");
		}
		if (vo.getSearchKeyword() == null) {
			vo.setSearchKeyword("");
		}

		System.out.println(memberservice.getAdminInfo(vo, cri));
		System.out.println(vo.getSearchCondition());
		System.out.println(vo.getSearchKeyword());
		// 검색, 키워드 값(페이징 처리시 필요)
		condition = vo.getSearchCondition();
		keyword = vo.getSearchKeyword();

		int total = memberservice.selectMyHouseMemberCount(vo);

		model.addAttribute("adminInfo", memberservice.getAdminInfo(vo, cri));
		model.addAttribute("pageMaker", new PageVO(cri, total));
		model.addAttribute("condition", vo.getSearchCondition());
		model.addAttribute("keyword", vo.getSearchKeyword());

		return "view/admin/admin_member_list";
	}

	@PostMapping(value = "/upload/uploadForm")
	public String uploadForm(@AuthenticationPrincipal SecurityUser user, HttpServletRequest request,
			MultipartHttpServletRequest profile) throws Exception {
		MemberVO member = memberservice.getMemberInfo(user.getId());
		FileUtils fileUtils = new FileUtils();
		List<MemberVO> list = fileUtils.parseProfileInfo(request, profile, member.getId());
		// 파일 저장된 위치
		String path = request.getSession().getServletContext().getRealPath("/") + "/profile/";

		// profile이 default.png가 아니면
		if (member.getProfile() != "default.png") {
			File file = new File(path + "\\" + member.getProfile());
			// 파일 삭제
			if (file.exists()) {
				file.delete();
			}
		}
		memberservice.updateProfile(list);

		return "redirect:/mypage";
	}

	// 게시판(공지사항, 도와주세요, 자유게시판) 목록
	@GetMapping("/community/{category}") //
	public String communtiyBoard(@PathVariable("category") String category, CommunityVO communityList,
			@AuthenticationPrincipal SecurityUser user, Model model, Criteria cri) {

		communityList.setNoticeCategory(category);
		//
		communityList.setNickname(user.getNickname());

		communityList.setId(user.getId());

		// 검색값 없을때 기본 값 설정
		if (communityList.getSearchCondition() == null) {
			communityList.setSearchCondition("NOTICE_TITLE");
		}
		if (communityList.getSearchKeyword() == null) {
			communityList.setSearchKeyword("");
		}

		// 검색, 키워드 값(페이징 처리시 필요)
		condition = communityList.getSearchCondition();
		keyword = communityList.getSearchKeyword();

		int total = communityService.selectCommunityCount(communityList);

		model.addAttribute("category", category);
		model.addAttribute("paramList", communityService.memberCommunityBoardList(communityList, cri));
		model.addAttribute("pageMaker", new PageVO(cri, total));
		model.addAttribute("condition", communityList.getSearchCondition());
		model.addAttribute("keyword", communityList.getSearchKeyword());

		System.out.println(communityService.memberCommunityBoardList(communityList, cri));

		return "view/member/mypage/member_community_boarder_list";
	}

	@RequestMapping(value = "/community/{category}") //
	@ResponseBody
	public String communtiyBoard1(@RequestParam Map<String, Object> parameters,
			@PathVariable("category") String category, CommunityVO communityList,
			@AuthenticationPrincipal SecurityUser user, Model model, Criteria cri)
			throws JsonMappingException, JsonProcessingException {

		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> hashMap = new HashMap<String, Object>();

		communityList.setNoticeCategory(category);
		//
		communityList.setNickname(user.getNickname());

		communityList.setId(user.getId());

		// 검색값 없을때 기본 값 설정
		// 검색값 없을때 기본 값 설정
		if (communityList.getSearchCondition() == null) {
			communityList.setSearchCondition("NOTICE_TITLE");
		}
		if (communityList.getSearchKeyword() == null) {
			communityList.setSearchKeyword("");
		}

		// 검색, 키워드 값(페이징 처리시 필요)
		condition = communityList.getSearchCondition();
		keyword = communityList.getSearchKeyword();

		int total = communityService.selectCommunityCount(communityList);

		model.addAttribute("category", category);
		model.addAttribute("paramList", communityService.memberCommunityBoardList(communityList, cri));
		model.addAttribute("pageMaker", new PageVO(cri, total));
		model.addAttribute("condition", communityList.getSearchCondition());
		model.addAttribute("keyword", communityList.getSearchKeyword());

		List<CommunityVO> paramList = communityService.memberCommunityBoardList(communityList, cri);
		hashMap.put("paramList", paramList);
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(hashMap);

		return json;
	}
}
