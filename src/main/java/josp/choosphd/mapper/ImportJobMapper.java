package josp.choosphd.mapper;

import josp.choosphd.domain.auth.ImportJobPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ImportJobMapper {

    @Select("""
        SELECT id, job_key AS jobKey, source_id AS sourceId, status,
               total_rows AS totalRows, processed_rows AS processedRows,
               inserted_rows AS insertedRows, updated_rows AS updatedRows,
               skipped_rows AS skippedRows, error_message AS errorMessage,
               started_at AS startedAt, finished_at AS finishedAt,
               created_at AS createdAt, updated_at AS updatedAt, deleted
        FROM import_job
        ORDER BY id DESC
    """)
    List<ImportJobPO> selectAll();

    @Select("""
        SELECT id FROM import_job
        WHERE job_key = #{jobKey} LIMIT 1
    """)
    Long findIdByJobKey(@Param("jobKey") String jobKey);

    @Insert("""
        INSERT INTO import_job (job_key, source_id, status, total_rows, processed_rows,
                                inserted_rows, updated_rows, skipped_rows,
                                error_message, started_at, finished_at, created_at)
        VALUES (#{jobKey}, #{sourceId}, #{status}, #{totalRows}, #{processedRows},
                #{insertedRows}, #{updatedRows}, #{skippedRows},
                #{errorMessage}, #{startedAt}, #{finishedAt}, #{createdAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ImportJobPO job);

    @Update("""
        UPDATE import_job SET status=#{status}, processed_rows=#{processedRows},
               inserted_rows=#{insertedRows}, updated_rows=#{updatedRows},
               skipped_rows=#{skippedRows}, error_message=#{errorMessage},
               finished_at=#{finishedAt}
        WHERE id=#{id}
    """)
    int update(ImportJobPO job);
}
