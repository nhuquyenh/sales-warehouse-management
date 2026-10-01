<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kiểm tra hệ thống</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<main class="status-card">
    <p class="eyebrow">Sprint 1 · Environment Check</p>
    <h1><c:out value="${healthStatus.applicationName()}" /></h1>
    <p class="status status--up">
        Trạng thái: <strong><c:out value="${healthStatus.status()}" /></strong>
    </p>
    <dl>
        <div>
            <dt>Java</dt>
            <dd><c:out value="${healthStatus.javaVersion()}" /></dd>
        </div>
        <div>
            <dt>Jakarta Servlet</dt>
            <dd><c:out value="${servletVersion}" /></dd>
        </div>
        <div>
            <dt>Ứng dụng</dt>
            <dd>Đã deploy thành công trên Tomcat.</dd>
        </div>
    </dl>
</main>
</body>
</html>
