/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private final OwnerRepository owners;

	private PetRepository petRepository;

	@Autowired
	public void setPetRepository(PetRepository petRepository) {
		this.petRepository = petRepository;
	}

	// 생성자를 사용하는 방법이 좋은 이유
	// 필수적으로 사용해야 하는 레퍼런스 없이는 이 인스턴스를 만들지 못하도록 강제
	// 한마디로 OwnerController는 OwnerRepository가 없으면 제대로 동작할 수 없다.!!
	// 단 순환의존이 될 경우는 생성자 인젝션 좋지 않음.
	public OwnerController(OwnerRepository clinicService) {
		this.owners = clinicService;
	}

	// 3. Setter를 활용
	// @Autowired
	// public void setOwners(OwnerRepository owners) {
	// this.owners = owners;
	// }

	// 2. 필드로 의존성을 주입받는 방법
	// @Autowired
	// private OwnerRepository owners;

	// 1. 생성자로 의존성을 주입받는 방법 -> 권장
	// 스프링 버전 4.3부터 어떤 클래스에 생성자가 1개 뿐이고 그 생성자에서 주입받는 레퍼런스 변수들이 Bean 으로 등록되어 있으면
	// Bean을 자동으로 주입하도록 됨. 그래서 @Autowired를 생략하고 사용 가능.
	/*
	 * @Autowired public OwnerController(OwnerRepository clinicService) { this.owners =
	 * clinicService; }
	 */

	// 1. Autowired 사용해서 꺼내기
	// 2. Applicationcontext 사용해서 직접꺼내기
	// @Autowired
	// private OwnerRepository owners;

	// private final ApplicationContext applicationContext;

	private VisitRepository visits;

	// // 의존성 주입은 빈끼리만 가능하다.
	// // OwnerRepository 없이는 OwnerController을 못 만들게 되어 있다.
	// public OwnerController(OwnerRepository clinicService) {
	// this.owners = clinicService;
	// this.applicationContext = applicationContext;
	// this.visits = visits;
	// }

	// @GetMapping("/bean")
	// @ResponseBody
	// // singlescope
	// public String bean() {
	// return "bean: " + applicationContext.getBean(OwnerRepository.class) + "\n" +
	// "owners: " + this.owners;
	// }

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@GetMapping("/owners/new")
	public String initCreationForm(Map<String, Object> model) {
		Owner owner = new Owner();
		model.put("owner", owner);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		else {
			this.owners.save(owner);
			return "redirect:/owners/" + owner.getId();
		}
	}

	@GetMapping("/owners/find")
	public String initFindForm(Map<String, Object> model) {
		model.put("owner", new Owner());
		return "owners/findOwners";
	}

	@GetMapping("/owners")
	public String processFindForm(Owner owner, BindingResult result, Map<String, Object> model) {

		// allow parameterless GET request for /owners to return all records
		if (owner.getFirstName() == null) {
			owner.setFirstName(""); // empty string signifies broadest possible search
		}

		// find owners by first name
		Collection<Owner> results = this.owners.findByFirstName(owner.getFirstName());
		if (results.isEmpty()) {
			// no owners found
			result.rejectValue("firstName", "notFound", "not found");
			return "owners/findOwners";
		}
		else if (results.size() == 1) {
			// 1 owner found
			owner = results.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}
		else {
			// multiple owners found
			model.put("selections", results);
			return "owners/ownersList";
		}
	}

	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Model model) {
		Owner owner = this.owners.findById(ownerId);
		model.addAttribute(owner);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result,
			@PathVariable("ownerId") int ownerId) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		else {
			owner.setId(ownerId);
			this.owners.save(owner);
			return "redirect:/owners/{ownerId}";
		}
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Owner owner = this.owners.findById(ownerId);
		for (Pet pet : owner.getPets()) {
			pet.setVisitsInternal(visits.findByPetId(pet.getId()));
		}
		mav.addObject(owner);
		return mav;
	}

}
