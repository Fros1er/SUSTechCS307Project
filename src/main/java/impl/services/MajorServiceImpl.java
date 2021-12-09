package impl.services;

import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.service.MajorService;

import java.util.List;

public class MajorServiceImpl implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void removeMajor(int majorId) {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<Major> getAllMajors() {
        throw new UnsupportedOperationException();

    }

    @Override
    public Major getMajor(int majorId) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        throw new UnsupportedOperationException();

    }
}
