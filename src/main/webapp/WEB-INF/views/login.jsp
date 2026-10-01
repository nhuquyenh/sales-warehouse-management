<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập | Hệ thống bán hàng &amp; quản lý kho</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/login.css">
    <script src="${pageContext.request.contextPath}/js/login.js" defer></script>
</head>
<body>
<main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
        <div class="brand-mark" aria-hidden="true">SW</div>
        <p class="eyebrow">Sales Warehouse</p>
        <h1 id="login-title">Hệ thống bán hàng &amp; quản lý kho</h1>
        <p class="subtitle">Đăng nhập để tiếp tục vào hệ thống.</p>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert--error" role="alert">
                <c:out value="${errorMessage}" />
            </div>
        </c:if>

        <form class="login-form" method="post" action="${pageContext.request.contextPath}/login">
            <div class="field">
                <label for="username">Tên đăng nhập</label>
                <input
                        id="username"
                        name="username"
                        type="text"
                        value="<c:out value='${loginUsername}' />"
                        autocomplete="username"
                        maxlength="50"
                        required
                        autofocus>
            </div>

            <div class="field">
                <label for="password">Mật khẩu</label>
                <div class="password-control">
                    <input
                            id="password"
                            name="password"
                            type="password"
                            autocomplete="current-password"
                            required>
                    <button
                            class="password-toggle"
                            id="password-toggle"
                            type="button"
                            aria-controls="password"
                            aria-pressed="false">
                        Hiện
                    </button>
                </div>
            </div>

            <button class="submit-button" type="submit">Đăng nhập</button>
        </form>
    </section>
</main>
</body>
</html>
