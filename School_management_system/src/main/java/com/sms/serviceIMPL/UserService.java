package com.sms.serviceIMPL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sms.model.ClassEntity;
import com.sms.model.FeeStructure;
import com.sms.model.Role;
import com.sms.model.School;
import com.sms.model.Student;
import com.sms.model.StudentFee;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.ClassRepository;
import com.sms.repository.FeeStructureRepository;
import com.sms.repository.RoleRepository;
import com.sms.repository.SchoolRepository;
import com.sms.repository.StudentFeeRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;
import com.sms.serviceInterface.UserServiceInterface;

import jakarta.transaction.Transactional;

@Service
public class UserService implements UserServiceInterface{
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private TeacherRepository teacherRepository;
	
	@Autowired
	private SchoolRepository schoolRepository;
	
	@Autowired
	private ClassRepository classRepository;
	
	@Autowired
	private StudentRepository studentRepository;
	
	@Autowired
	private FeeStructureRepository feeStructureRepository;
	
	@Autowired
	private StudentFeeRepository studentFeeRepository;

	
	public User getCurrentUser() {
	    String email = SecurityContextHolder.getContext()
	            .getAuthentication().getName();

	    return userRepository.findByEmail(email);
//	            .orElseThrow(() -> new RuntimeException("User not found"));
	}
	
	
	public void initializeClasses(Integer schoolId) {

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        List<String> classNames = Arrays.asList(
                "Play Group", "LKG", "UKG",
                "1", "2", "3", "4", "5", "6",
                "7", "8", "9", "10", "11", "12"
        );

        List<String> sections = Arrays.asList("A", "B", "C", "D");

        for (String className : classNames) {
            for (String section : sections) {

                ClassEntity classEntity = new ClassEntity();
                classEntity.setClassName(className);
                classEntity.setSection(section);
                classEntity.setSchool(school);

                if (!classRepository.existsByClassNameAndSectionAndSchool(
                        className, section, school)) {

                    classRepository.save(classEntity);
                }
            }
        }
    }
	@Override
	public void initializeClasses() {
		initializeClasses(getCurrentUser().getSchool().getId());
	}
	
	
	@Override
	public String schoolName() {
		return getCurrentUser().getSchool().getName();
	}
	
//	TEACHER--------------------------------------------------------------------------
	
	@Override
	public void addTeacher(Teacher teacher, String role) {
	
		teacher.setRole(roleRepository.findByName(role));
		teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
		teacher.setSchool(getCurrentUser().getSchool());
		
		teacherRepository.save(teacher);
	}
	

	@Override
	public List<Teacher> findAllTeachers() {
		return teacherRepository.findBySchoolId(getCurrentUser().getSchool().getId());
	}
	
	@Override
	public Optional<Teacher> findTeacherById(int id) {
		return teacherRepository.findById(id);
	}
	
	

//CLASS---------------------------------------------------------------------------------------------------
	
	@Override
	public List<ClassEntity> findAllClasses() {
		return classRepository.findBySchoolId(getCurrentUser().getSchool().getId());
	}
	
	@Override
	public ClassEntity findClassByClsAndSec(String cls, String sec) {
		
		return classRepository.findByClassNameAndSectionAndSchoolId(cls,sec,getCurrentUser().getSchool().getId());
	}
	
	@Override
	public Optional<ClassEntity> findClassById(int id) {
		return classRepository.findById(id);
	} 

	@Override
	public void assignTeacherToClass(int classId, int teacherId) {
		
		Optional<ClassEntity> classEntity = findClassById(classId);
		
		Optional<Teacher> teacher = findTeacherById(teacherId);
		
		classEntity.get().setClassTeacher(teacher.get());
				
		classRepository.save(classEntity.get());
	}
	
//SET FEE OF CLASSES---------------------------------------------------------------------------------------------------


	@Override
	public void addStudent(Student student,String role,String classname,String sec) {
		
		
		student.setAdmissionDate(LocalDate.now());
		student.setPassword(passwordEncoder.encode(student.getPassword()));
		student.setSchool(getCurrentUser().getSchool());
		student.setUser(getCurrentUser());
		student.setRole(roleRepository.findByName(role));
		student.setClassEntity(classRepository.findByClassNameAndSectionAndSchoolId(classname,sec,getCurrentUser().getSchool().getId()));
				
		studentRepository.save(student);
		
	}

	@Override
	public List<Student> findAllStudent() {
		return studentRepository.findAllBySchoolId(getCurrentUser().getSchool().getId());
	}

	
	@Override
	public List<Student> findAllStudentByClsAndSec(String cls, String sec) {
		return studentRepository.findByClassEntityIdAndSchoolId(findClassByClsAndSec(cls, sec).getId(),getCurrentUser().getSchool().getId());
	}

	@Override
	public List<Student> findAllStudentByCls(String cls) {
		return studentRepository.findByClassEntityClassNameAndSchoolId(cls,getCurrentUser().getSchool().getId());
//		return null;
	}

	@Override
	public List<Student> findAllStudentBySec(String sec) {
		return studentRepository.findByClassEntitySectionAndSchoolId(sec,getCurrentUser().getSchool().getId());
	}

	
//SET FEE OF CLASSES---------------------------------------------------------------------------------------------------

	@Transactional
    public void initializeDefaultFees(School school) {

        List<ClassEntity> classes = classRepository.findBySchoolId(getCurrentUser().getSchool().getId());

        for (ClassEntity classEntity : classes) {

            boolean exists = feeStructureRepository
                    .existsBySchoolAndClassEntityAndFeeTypeAndSession(
                            school,
                            classEntity,
                            "Monthly Fee",
                            "2026-2027"
                    );

            if (!exists) {

                FeeStructure fee = FeeStructure.builder()
                        .school(school)
                        .classEntity(classEntity)
                        .feeType("Monthly Fee")
                        .amount(new BigDecimal("0"))
                        .session("2026-2027")
                        .build();

                feeStructureRepository.save(fee);
            }
        }
    }

	@Override
	public void initializeFee() {
		initializeDefaultFees(getCurrentUser().getSchool());
	}


	@Override
	public List<FeeStructure> findAllFeeStructureBySchool() {
		return feeStructureRepository.findAllFeeStructureBySchool(getCurrentUser().getSchool());
	}


	@Override
	public void updateFeeStructure(int id, String type, int amount) {
	
		School school = getCurrentUser().getSchool();

	    FeeStructure feeStructure = feeStructureRepository
	            .findBySchoolAndClassEntityIdAndFeeType(
	                    school,
	                    id,
	                    type)
	            .orElseThrow(() -> new RuntimeException("Fee structure not found"));
	    
	    feeStructure.setAmount(new BigDecimal(amount));
	    
	    feeStructureRepository.save(feeStructure);
		
	}



	
	
	
}
