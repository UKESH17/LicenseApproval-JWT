package com.htc.licenseapproval.service.implement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.htc.licenseapproval.dto.BUResponseDTO;
import com.htc.licenseapproval.dto.CoursesDTO;
import com.htc.licenseapproval.dto.LicenseApprovalDTO;
import com.htc.licenseapproval.dto.NewRequestListDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.dto.ResponseDTO;
import com.htc.licenseapproval.entity.BUdetails;
import com.htc.licenseapproval.entity.LicenseDetails;
import com.htc.licenseapproval.entity.LicenseLogMessages;
import com.htc.licenseapproval.entity.RequestDetails;
import com.htc.licenseapproval.entity.RequestHeader;
import com.htc.licenseapproval.entity.UploadedFile;
import com.htc.licenseapproval.enums.LicenceStatus;
import com.htc.licenseapproval.enums.LicenseType;
import com.htc.licenseapproval.enums.Status;
import com.htc.licenseapproval.file.compressor.Compressor;
import com.htc.licenseapproval.mapper.MapperService;
import com.htc.licenseapproval.repository.BUdetailsRepository;
import com.htc.licenseapproval.repository.LicenseLogMessagesRepository;
import com.htc.licenseapproval.repository.RequestDetailsRepository;
import com.htc.licenseapproval.repository.RequestHeaderRepository;
import com.htc.licenseapproval.service.RequestHeaderService;
import com.htc.licenseapproval.utils.DateFormatter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RequestHeaderServiceImplement implements RequestHeaderService {

	@Autowired
	private RequestHeaderRepository requestHeaderRepository;

	@Autowired
	private MapperService mapperService;

	@Autowired
	private BUdetailsRepository bUdetailsRepository;

	@Autowired
	private RequestDetailsRepository requestDetailsRepository;

	@Autowired
	private LicenseLogMessagesRepository licenseLogMessagesRepository;

	@Autowired
	private Compressor compressor;

	/* FIND */

	@Override
	public RequestHeader findById(String requestId) {
		return requestHeaderRepository.findById(requestId)
				.orElseThrow(() -> new RuntimeException("Request header id not found"));
	}

	/* REQUEST CREATE AND UPDATE */

	@Override
	public RequestResponseDTO newRequestHeader(NewRequestListDTO newRequestListDTO) {

		RequestHeader requestHeader = mapperService.toRequestList(newRequestListDTO);

		BUdetails budetails = bUdetailsRepository.findByBu(newRequestListDTO.getBuDetails().getBu())
				.orElseThrow(() -> new RuntimeException("BU not found"));

		if (newRequestListDTO.getExcelFile() != null) {
			requestHeader.setExcelFile(newRequestListDTO.getExcelFile());
		}

		Set<RequestDetails> requestDetails = new HashSet<>();

		requestHeader.setBuDetails(budetails);
		for (RequestDetailsDTO detailDTO : newRequestListDTO.getRequestDetails()) {
			RequestDetails request = mapperService.toRequestDetails(detailDTO);
			request.setRequestId(null);
			request.setApprovalGivenBy("Not yet approved");
			LicenseDetails licenseDetails = new LicenseDetails();
			licenseDetails.setLicenseType(detailDTO.getLicenseDetails().getLicenseType());
			licenseDetails.setLicenceStatus(LicenceStatus.PENDING);
			licenseDetails.setRequestedDate(DateFormatter.normaliseDate(LocalDateTime.now()));
			request.setLicenseDetails(licenseDetails);
			request.setStatus(Status.PENDING);
			requestDetails.add(request);
		}

		requestHeader.setRequestDetails(requestDetails);

		RequestHeader header = requestHeaderRepository.save(requestHeader);

		for (RequestDetails details : header.getRequestDetails()) {
			details.setRequestHeader(header);
			requestDetailsRepository.save(details);
		}

		Set<RequestHeader> set = budetails.getRequests();
		if (set == null) {
			set = new HashSet<>();
		}

		set.add(requestHeader);
		budetails.setRequests(set);
		bUdetailsRepository.save(budetails);

		return mapperService.toResponseDTO(header);
	}

	@Override
	public LicenseApprovalDTO requestApproval(String id, Status status) {

		LicenseApprovalDTO approvalDTO = new LicenseApprovalDTO();
		Set<LicenseDetails> licenseDetailSet = new HashSet<>();
		String username = SecurityContextHolder.getContext().getAuthentication().getName();

		RequestHeader request = this.findById(id);
		if (request != null) {

			switch (status) {

			case APPROVED: {

				Set<RequestDetails> requestDetailSet = request.getRequestDetails();

				for (RequestDetails requestDetails : requestDetailSet) {

					requestDetails.setStatus(status);
					LicenseDetails licenseDetails = requestDetails.getLicenseDetails();
					licenseDetails.setLicenceStatus(LicenceStatus.ACTIVE);
					LocalDateTime start = request.getLicenseRequiredDate();
					LocalDateTime end = start.plusMonths(1);
					licenseDetails.setLicenseStartedDate(start);
					licenseDetails.setLicenseExpireDate(end);
					licenseDetailSet.add(licenseDetails);

					requestDetails.setApprovalGivenBy(username);
					requestDetails.setLicenseDetails(licenseDetails);

					LicenseLogMessages logMessages = new LicenseLogMessages();
					logMessages.setLoggedDate(DateFormatter.normaliseDate(LocalDateTime.now()));
					logMessages.setLogMessages("Request Approval Status -> " + status + " for request details id " + id
							+ " by " + username);
					logMessages.setRequestDetails(requestDetails);
					licenseLogMessagesRepository.save(logMessages);

				}

				approvalDTO.setRequestHeaderId(id);
				approvalDTO.setLicenseDetailsDTOs(
						licenseDetailSet.stream().map(mapperService::toLicenseDTO).collect(Collectors.toSet()));
				approvalDTO.setMessage("License request approved");
				approvalDTO.setApprovalGivenBy(username);

				request.setRequestDetails(requestDetailSet);

				break;
			}
			case REJECTED: {

				Set<RequestDetails> requestDetailSet = request.getRequestDetails();

				for (RequestDetails requestDetails : requestDetailSet) {

					requestDetails.setStatus(status);
					requestDetails.setApprovalGivenBy("Rejected by - " + username);
					LicenseDetails licenseDetails = requestDetails.getLicenseDetails();
					licenseDetails.setLicenceStatus(LicenceStatus.PENDING);
					licenseDetails.setLicenseStartedDate(null);
					licenseDetails.setLicenseExpireDate(null);
					request.setRequestDetails(requestDetailSet);

					LicenseLogMessages logMessages = new LicenseLogMessages();
					logMessages.setLoggedDate(DateFormatter.normaliseDate(LocalDateTime.now()));
					logMessages.setLogMessages("Request Approval Status -> " + status + " for request details id " + id
							+ " by " + username);
					logMessages.setRequestDetails(requestDetails);
					licenseLogMessagesRepository.save(logMessages);

				}

				approvalDTO.setRequestHeaderId(id);
				approvalDTO.setLicenseDetailsDTOs(
						licenseDetailSet.stream().map(mapperService::toLicenseDTO).collect(Collectors.toSet()));
				approvalDTO.setMessage("License request rejected");
				approvalDTO.setApprovalGivenBy("Rejected by - " + username);

				request.setRequestDetails(requestDetailSet);

				break;
			}
			case PENDING: {

				Set<RequestDetails> requestDetailSet = request.getRequestDetails();

				for (RequestDetails requestDetails : requestDetailSet) {

					requestDetails.setStatus(status);
					LicenseDetails licenseDetails = requestDetails.getLicenseDetails();
					licenseDetails.setLicenceStatus(LicenceStatus.PENDING);
					licenseDetails.setLicenseStartedDate(null);
					licenseDetails.setLicenseExpireDate(null);
					request.setRequestDetails(requestDetailSet);

					LicenseLogMessages logMessages = new LicenseLogMessages();
					logMessages.setLoggedDate(DateFormatter.normaliseDate(LocalDateTime.now()));
					logMessages.setLogMessages("Request Approval Status -> " + status + " for request details id " + id
							+ " by " + username);
					logMessages.setRequestDetails(requestDetails);
					licenseLogMessagesRepository.save(logMessages);

				}

				approvalDTO.setRequestHeaderId(id);
				approvalDTO.setLicenseDetailsDTOs(
						licenseDetailSet.stream().map(mapperService::toLicenseDTO).collect(Collectors.toSet()));
				approvalDTO.setMessage("License request Pending");
				approvalDTO.setApprovalGivenBy("Not yet Approved");
				request.setRequestDetails(requestDetailSet);

				break;
			}

			default:
				throw new IllegalArgumentException("Unexpected value: " + status);
			}

			requestHeaderRepository.save(request);

			return approvalDTO;
		}

		throw new RuntimeException("Request not found");
	}

	/* FILE RETRIEVAL */

	@Override
	public byte[] getApprovalMail(String requestId) throws IOException {
		return compressor.decompress(this.findById(requestId).getApprovalMail().getFileData());
	}

	@Override
	public byte[] getExcelFile(String requestId) throws IOException {
		return compressor.decompress(this.findById(requestId).getExcelFile().getFileData());
	}

	/* ALL REQUEST DATA */

	// unused
	@Override
	public BUResponseDTO<List<RequestResponseDTO>> totalRequestPerBU(String name, LicenseType licenseType) {
		BUResponseDTO<List<RequestResponseDTO>> response = new BUResponseDTO<>();
		List<RequestResponseDTO> list = bUdetailsRepository.findByBu(name).orElse(new BUdetails()).getRequests()
				.stream()
				.flatMap(req -> req.getRequestDetails().stream()
						.filter(detail -> detail.getLicenseDetails().getLicenseType().equals(licenseType)
								&& !detail.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.PENDING))
						.map(mapperService::toResponseDTO))
				.collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		response.setTotalHoursSpent(findTotalhoursSpentPerBU(list));
		return response;
	}

	@Override
	public Map<String, BUResponseDTO<List<RequestResponseDTO>>> totalRequestForAllBUs(LicenseType licenseType) {

		Map<String, BUResponseDTO<List<RequestResponseDTO>>> buMap = new HashMap<>();

		for (BUdetails bu : bUdetailsRepository.findAll()) {

			buMap.put(bu.getBu(), this.totalRequestPerBU(bu.getBu(), licenseType));
		}

		return buMap;
	}

	private float findTotalhoursSpentPerBU(List<RequestResponseDTO> list) {

		float totalhrs = (float) list.parallelStream().flatMap((request) -> request.getRequestDetails().stream())
				.filter(t -> t.getCourses() != null).flatMap(t -> t.getCourses().stream())
				.mapToDouble(value -> value.getHoursSpent()).sum();

		return totalhrs;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> totalRequest() {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();

		List<RequestResponseDTO> list = requestHeaderRepository.findAll().stream().map(mapperService::toResponseDTO)
				.collect(Collectors.toList());
		response.setData(list);
		response.setCount(list.size());
		return response;

	}

	// unused
	@Override
	public List<RequestDetails> getAllRequestLists() {
		return this.requestDetailsRepository.findAll();
	}

	// unused
	@Override
	public ResponseDTO<List<RequestResponseDTO>> totalRequestsByEmp(Long empid) {

		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream().filter(t -> t.getEmpid().equals(empid))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;

	}

	/* LICENSE AND REQUEST STAUS METHOD */

	@Override
	public ResponseDTO<List<RequestDetails>> allConsumedLicense(LicenseType licenseType) {

		List<RequestDetails> list = this.getAllRequestLists().stream()
				.filter(t -> !t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.PENDING)
						&& !t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.EXPIRED)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.collect(Collectors.toList());
		ResponseDTO<List<RequestDetails>> response = new ResponseDTO<>();
		response.setData(list);
		response.setCount(list.size());
		return response;

	}

	private ResponseDTO<List<RequestDetails>> allConsumed() {

		List<RequestDetails> list = this.getAllRequestLists().stream()
				.filter(t -> !t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.PENDING))
				.collect(Collectors.toList());
		ResponseDTO<List<RequestDetails>> response = new ResponseDTO<>();
		response.setData(list);
		response.setCount(list.size());
		return response;

	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> allActiveLicense(LicenseType licenseType) {

		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream()
				.filter(t -> t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.ACTIVE)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> allPendingLicense(LicenseType licenseType) {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream()
				.filter(t -> t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.PENDING)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> allExpireSoonLicense(LicenseType licenseType) {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream()
				.filter(t -> t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.EXPIRING_SOON)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> allExpiredLicense(LicenseType licenseType) {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream()
				.filter(t -> t.getLicenseDetails().getLicenceStatus().equals(LicenceStatus.EXPIRED)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> pendingRequest(LicenseType licenseType) {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream().filter(
				t -> t.getStatus().equals(Status.PENDING) && t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> rejectedRequest(LicenseType licenseType) {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream()
				.filter(t -> t.getStatus().equals(Status.REJECTED)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public ResponseDTO<List<RequestResponseDTO>> approvedRequest(LicenseType licenseType) {
		ResponseDTO<List<RequestResponseDTO>> response = new ResponseDTO<>();
		List<RequestResponseDTO> list = this.getAllRequestLists().stream()
				.filter(t -> t.getStatus().equals(Status.APPROVED)
						&& t.getLicenseDetails().getLicenseType().equals(licenseType))
				.map(mapperService::toResponseDTO).collect(Collectors.toList());

		response.setData(list);
		response.setCount(list.size());
		return response;
	}

	@Override
	public Map<Month, List<RequestResponseDTO>> quarterlyReport(LicenseType licenseType) {
		Month present = LocalDateTime.now().getMonth();
		List<RequestDetails> allActiveLicense = this.allConsumedLicense(licenseType).getData();
		Map<Month, List<RequestResponseDTO>> quarterlyReport = new HashMap<>();
		for (int i = 0; i < 3; i++) {
			quarterlyReport.put(present.minus(i), new ArrayList<>());
		}

		for (RequestDetails request : allActiveLicense) {

			Month monthKey = request.getLicenseDetails().getLicenseStartedDate().getMonth();

			if (quarterlyReport.containsKey(monthKey)) {

				List<RequestResponseDTO> list = quarterlyReport.get(monthKey);
				list.add(mapperService.toResponseDTO(request));

				quarterlyReport.put(monthKey, list);

			}

		}

		return quarterlyReport;
	}

	@Override
	public Map<Month, List<RequestResponseDTO>> quarterlyReportBYBU(String BU, LicenseType licenseType) {
		bUdetailsRepository.findByBu(BU).orElseThrow(() -> new RuntimeException("bu not found"));
		Month present = LocalDateTime.now().getMonth();
		List<RequestDetails> allActiveLicense = this.allConsumedLicense(licenseType).getData().stream()
				.filter(req -> req.getRequestHeader().getBuDetails().getBu().equalsIgnoreCase(BU))
				.collect(Collectors.toList());

		Map<Month, List<RequestResponseDTO>> quarterlyReport = new HashMap<>();

		for (int i = 0; i < 3; i++) {
			quarterlyReport.put(present.minus(i), new ArrayList<>());
		}

		for (RequestDetails request : allActiveLicense) {

			Month monthKey = request.getLicenseDetails().getLicenseStartedDate().getMonth();

			if (quarterlyReport.containsKey(monthKey)) {

				List<RequestResponseDTO> list = quarterlyReport.get(monthKey);
				list.add(mapperService.toResponseDTO(request));

				quarterlyReport.put(monthKey, list);

			}

		}

		return quarterlyReport;
	}

	@Override
	public Map<Month, List<RequestResponseDTO>> annualReport(LicenseType licenseType) {
		Map<Month, List<RequestResponseDTO>> annualReport = new LinkedHashMap<>();

		for (Month month : Month.values()) {
			annualReport.put(month, new ArrayList<>());
		}
		List<RequestDetails> allActiveLicense = this.allConsumedLicense(licenseType).getData();

		for (RequestDetails request : allActiveLicense) {

			Month monthKey = request.getLicenseDetails().getLicenseStartedDate().getMonth();

			if (annualReport.containsKey(monthKey)) {

				List<RequestResponseDTO> list = annualReport.get(monthKey);
				list.add(mapperService.toResponseDTO(request));

				annualReport.put(monthKey, list);

			}

		}

		return annualReport;
	}

	@Override

	public Map<Month, Map<String, List<RequestDetailsDTO>>> quarterlyReportPerBU(LicenseType licenseType) {

		Map<Month, Map<String, List<RequestDetailsDTO>>> quarterlyReport = new HashMap<>();

		Month present = LocalDateTime.now().getMonth();

		for (int i = 0; i < 3; i++) {

			Map<String, List<RequestDetailsDTO>> buMap = new HashMap<>();

			for (BUdetails bu : bUdetailsRepository.findAll()) {

				buMap.put(bu.getBu(), new ArrayList<>());
			}
			quarterlyReport.put(present.minus(i), buMap);
		}
		List<RequestDetails> allActiveLicense = this.allConsumedLicense(licenseType).getData();

		for (RequestDetails request : allActiveLicense) {

			Month monthKey = request.getLicenseDetails().getLicenseStartedDate().getMonth();

			String bu = request.getRequestHeader().getBuDetails().getBu();

			if (quarterlyReport.containsKey(monthKey)) {

				Map<String, List<RequestDetailsDTO>> map = quarterlyReport.get(monthKey);

				if (map.containsKey(bu)) {
					List<RequestDetailsDTO> list = map.get(bu);

					list.add(mapperService.toRequestDetailsDTO(request));

					map.put(bu, list);
				}
				quarterlyReport.put(monthKey, map);

			}

		}

		return quarterlyReport;

	}

	@Override
	public Map<Month, Map<String, List<RequestDetailsDTO>>> quarterlyReportperQuater(LicenseType licenseType,
			String quarter) {

		List<RequestDetails> allActiveLicense = this.allConsumedLicense(licenseType).getData();
		Map<Month, Map<String, List<RequestDetailsDTO>>> quarterlyReport = new LinkedHashMap<>();

		Map<String, List<Month>> quarterMap = Map.of("Q1", List.of(Month.JANUARY, Month.FEBRUARY, Month.MARCH), "Q2",
				List.of(Month.APRIL, Month.MAY, Month.JUNE), "Q3", List.of(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
				"Q4", List.of(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER));
		List<Month> selectedMonths = quarterMap.getOrDefault(quarter.toUpperCase(), new ArrayList<>());

		for (Month month : selectedMonths) {
			Map<String, List<RequestDetailsDTO>> buMap = new HashMap<>();
			for (BUdetails bu : bUdetailsRepository.findAll()) {
				buMap.put(bu.getBu(), new ArrayList<>());
			}
			quarterlyReport.put(month, buMap);
		}

		for (RequestDetails request : allActiveLicense) {

			Month monthKey = request.getLicenseDetails().getLicenseStartedDate().getMonth();

			String bu = request.getRequestHeader().getBuDetails().getBu();

			if (quarterlyReport.containsKey(monthKey)) {

				Map<String, List<RequestDetailsDTO>> map = quarterlyReport.get(monthKey);

				if (map.containsKey(bu)) {
					List<RequestDetailsDTO> list = map.get(bu);

					list.add(mapperService.toRequestDetailsDTO(request));

					map.put(bu, list);
				}
				quarterlyReport.put(monthKey, map);

			}

		}

		return quarterlyReport;
	}

	@Scheduled(fixedDelay = 43200000)
	void updateLicenseStatus() {
		List<RequestDetails> activeLicenses = this.allConsumed().getData();

		if (!activeLicenses.isEmpty()) {
			LocalDateTime now = LocalDateTime.now();
			activeLicenses.forEach(license -> {
				statusUpdate(license, now);
			});
		}
	}

	void statusUpdate(RequestDetails license, LocalDateTime now) {
		try {
			LocalDateTime expiryDate = license.getLicenseDetails().getLicenseExpireDate();
			LicenceStatus currentStatus = license.getLicenseDetails().getLicenceStatus();
			LicenceStatus newStatus = currentStatus;

			long daysRemaining = ChronoUnit.DAYS.between(now, expiryDate);

			if (daysRemaining < 0 && currentStatus != LicenceStatus.EXPIRED) {
				newStatus = LicenceStatus.EXPIRED;
			} else if (daysRemaining <= 5 && currentStatus == LicenceStatus.ACTIVE) {
				newStatus = LicenceStatus.EXPIRING_SOON;
			} else if (daysRemaining > 5 && currentStatus != LicenceStatus.ACTIVE) {
				newStatus = LicenceStatus.ACTIVE;
			}

			if (!newStatus.equals(currentStatus)) {
				license.getLicenseDetails().setLicenceStatus(newStatus);
				log.info("License status changed from {} to {}", currentStatus, newStatus);
				requestDetailsRepository.save(license);
			}

		} catch (Exception e) {
			log.error("Error updating license ID {}: {}", license.getRequestId(), e.getMessage());
		}
	}

}
