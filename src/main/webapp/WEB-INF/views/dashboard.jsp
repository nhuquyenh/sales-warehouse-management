<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard | Hệ thống bán hàng &amp; quản lý kho</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
</head>
<body>
<main class="dashboard-page">
    <section class="dashboard-card">
        <span class="success-badge">Đăng nhập thành công</span>
        <h1>Xin chào, <c:out value="${authenticatedUser.fullName()}" /></h1>
        <dl>
            <div>
                <dt>Username</dt>
                <dd><c:out value="${authenticatedUser.username()}" /></dd>
            </div>
            <div>
                <dt>Họ và tên</dt>
                <dd><c:out value="${authenticatedUser.fullName()}" /></dd>
            </div>
            <div>
                <dt>Vai trò</dt>
                <dd><c:out value="${roleDisplay}" /></dd>
            </div>
        </dl>
        <button class="logout-placeholder" type="button" disabled>
            Đăng xuất — triển khai tại S1-02
        </button>
    </section>
</main>
</body>
</html>
