package impl.services;

import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class SemesterServiceImpl implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        if (end.before(begin)) throw new IntegrityViolationException();
        return update("INSERT INTO semester (id, name, begin, \"end\") VALUES (DEFAULT, ?, ?, ?)",
                stmt -> {
                    stmt.setString(1, name);
                    stmt.setDate(2, begin);
                    stmt.setDate(3, end);
                });
    }

    @Override
    public void removeSemester(int semesterId) {
        //TODO: Remove related courses
        update(
                "DELETE FROM semester WHERE id = ?",
                stmt -> stmt.setInt(1, semesterId)
        );
    }

    @Override
    public List<Semester> getAllSemesters() {
        List<Semester> res = new ArrayList<>();
        safeSelect("SELECT * FROM semester",
                resultSet -> {
                    Semester s = new Semester();
                    s.id = resultSet.getInt(1);
                    s.name = resultSet.getString(2);
                    s.begin = resultSet.getDate(3);
                    s.end = resultSet.getDate(4);
                    res.add(s);
                });
        return res;
    }

    @Override
    public Semester getSemester(int semesterId) {
        Semester s = new Semester();
        if (!safeSelect("SELECT * FROM semester where id = ?",
                stmt -> stmt.setInt(1, semesterId),
                resultSet -> {
                    s.id = resultSet.getInt(1);
                    s.name = resultSet.getString(2);
                    s.begin = resultSet.getDate(3);
                    s.end = resultSet.getDate(4);
                })) {
            throw new EntityNotFoundException();
        }
        return s;
    }
}
