package co.jueyi.geekshop.service;

import co.jueyi.geekshop.common.utils.BeanMapper;
import co.jueyi.geekshop.entity.*;
import co.jueyi.geekshop.exception.EntityNotFoundException;
import co.jueyi.geekshop.mapper.AdministratorEntityMapper;
import co.jueyi.geekshop.mapper.AuthenticationMethodEntityMapper;
import co.jueyi.geekshop.mapper.UserEntityMapper;
import co.jueyi.geekshop.mapper.UserRoleJoinEntityMapper;
import co.jueyi.geekshop.options.SuperadminCredentials;
import co.jueyi.geekshop.service.helper.QueryHelper;
import co.jueyi.geekshop.service.helper.ServiceHelper;
import co.jueyi.geekshop.types.administrator.*;
import co.jueyi.geekshop.types.common.DeletionResponse;
import co.jueyi.geekshop.types.common.DeletionResult;
import co.jueyi.geekshop.types.role.Role;
import co.jueyi.geekshop.types.user.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class AdministratorService {
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final UserEntityMapper userEntityMapper;
    private final AdministratorEntityMapper administratorEntityMapper;
    private final UserRoleJoinEntityMapper userRoleJoinEntityMapper;
    private final AuthenticationMethodEntityMapper authenticationMethodEntityMapper;
    private final UserService userService;
    private final RoleService roleService;

    @PostConstruct
    public void initAdministrators() {
        this.ensureSuperAdminExists();
    }

    @SuppressWarnings("Duplicates")
    public AdministratorList findAll(AdministratorListOptions options) {
        Pair<Integer, Integer> currentAndSize = ServiceHelper.getListOptions(options);
        IPage<AdministratorEntity> page = new Page<>(currentAndSize.getLeft(), currentAndSize.getRight());
        QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<AdministratorEntity> administratorEntityPage =
                this.administratorEntityMapper.selectPage(page, queryWrapper);

        AdministratorList administratorList = new AdministratorList();
        administratorList.setTotalItems((int) administratorEntityPage.getTotal());

        if (CollectionUtils.isEmpty(administratorEntityPage.getRecords())) return administratorList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        administratorEntityPage.getRecords().forEach(administratorEntity -> {
            Administrator administrator = BeanMapper.map(administratorEntity, Administrator.class);
            administratorList.getItems().add(administrator);
        });

        return administratorList;
    }

    private void buildFilter(QueryWrapper queryWrapper, AdministratorFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getFirstName(), "first_name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getLastName(), "last_name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getEmailAddress(), "email_address");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }


    private void buildSortOrder(QueryWrapper queryWrapper, AdministratorSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getFirstName(), "first_name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getLastName(), "last_name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }


    public Administrator findOne(Long administratorId) {
        AdministratorEntity administratorEntity = this.administratorEntityMapper.selectById(administratorId);
        if (administratorEntity == null) return null;
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    public Administrator findOneByUserId(Long userId) {
        QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AdministratorEntity::getUserId, userId).isNull(AdministratorEntity::getDeletedAt);
        AdministratorEntity administratorEntity = this.administratorEntityMapper.selectOne(queryWrapper);
        if (administratorEntity == null) return null;
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    @Transactional
    public Administrator create(CreateAdministratorInput input) {
        AdministratorEntity administratorEntity = BeanMapper.map(input, AdministratorEntity.class);
        User user = this.userService.createAdminUser(input.getEmailAddress(), input.getPassword());
        administratorEntity.setUserId(user.getId());
        this.administratorEntityMapper.insert(administratorEntity);
        input.getRoleIds().forEach(roleId -> this.assignRole(administratorEntity.getId(), roleId));
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    public Administrator update(UpdateAdministratorInput input) {
        AdministratorEntity administratorEntity =
                ServiceHelper.getEntityOrThrow(this.administratorEntityMapper, input.getId());
        BeanMapper.patch(input, administratorEntity);
        this.administratorEntityMapper.updateById(administratorEntity);

        if (!StringUtils.isEmpty(input.getPassword())) {
            AuthenticationMethodEntity nativeAuthMethodEntity =
                    userService.getNativeAuthMethodEntityByUserId(administratorEntity.getUserId());
            nativeAuthMethodEntity.setPasswordHash(this.passwordEncoder.encode(input.getPassword()));
            this.authenticationMethodEntityMapper.updateById(nativeAuthMethodEntity);
        }
        if (!CollectionUtils.isEmpty(input.getRoleIds())) {
            input.getRoleIds().forEach(roleId -> this.assignRole(administratorEntity.getId(), roleId));
        }
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    /**
     * Assigns a Role to the Administrator's User entity.
     */
    public Administrator assignRole(Long administratorId, Long roleId) {
        AdministratorEntity administratorEntity = this.administratorEntityMapper.selectById(administratorId);
        if (administratorEntity == null) throw new EntityNotFoundException("Administrator", administratorId);
        Role role = this.roleService.findOne(roleId);
        if (role == null) throw new EntityNotFoundException("Role", roleId);
        QueryWrapper<UserRoleJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRoleJoinEntity::getRoleId, roleId)
                .eq(UserRoleJoinEntity::getUserId, administratorEntity.getUserId());
        if (this.userRoleJoinEntityMapper.selectCount(queryWrapper) == 0) { // 确保不存在
            UserRoleJoinEntity userRoleJoinEntity = new UserRoleJoinEntity();
            userRoleJoinEntity.setRoleId(roleId);
            userRoleJoinEntity.setUserId(administratorEntity.getUserId());
            this.userRoleJoinEntityMapper.insert(userRoleJoinEntity);
        }
        return BeanMapper.map(administratorEntity, Administrator.class);
    }

    public DeletionResponse softDelete(Long id) {
        AdministratorEntity administratorEntity =
                ServiceHelper.getEntityOrThrow(this.administratorEntityMapper, id);
        administratorEntity.setDeletedAt(new Date());
        this.administratorEntityMapper.updateById(administratorEntity);
        this.userService.softDelete(administratorEntity.getUserId());
        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setResult(DeletionResult.DELETED);
        return deletionResponse;
    }

    /**
     * There must always exists a SuperAdmin, otherwise full administration via API will
     * no longer be possible.
     */
    private void ensureSuperAdminExists() {
        SuperadminCredentials superadminCredentials =
                this.configService.getAuthOptions().getSuperadminCredentials();

        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getIdentifier, superadminCredentials.getIdentifier());
        UserEntity superAdminUserEntity = userEntityMapper.selectOne(queryWrapper);
        if (superAdminUserEntity == null) {
            RoleEntity superAdminRole = this.roleService.getSuperAdminRoleEntity();
            CreateAdministratorInput input = new CreateAdministratorInput();
            input.setEmailAddress(superadminCredentials.getIdentifier());
            input.setPassword(superadminCredentials.getPassword());
            input.setFirstName("Super");
            input.setLastName("Admin");
            input.getRoleIds().add(superAdminRole.getId());
            this.create(input);
        }
    }
}