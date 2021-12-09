package impl.services;

import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.service.SemesterService;

import java.sql.Date;
import java.util.List;

public class SemesterServiceImpl implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void removeSemester(int semesterId) {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<Semester> getAllSemesters() {
        throw new UnsupportedOperationException();

    }

    @Override
    public Semester getSemester(int semesterId) {
        throw new UnsupportedOperationException();

    }
}
