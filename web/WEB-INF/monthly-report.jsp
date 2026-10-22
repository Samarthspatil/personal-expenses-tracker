<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head><title>SpendWise Monthly Report</title></head>
<body>
<h1>SpendWise Monthly Report</h1>
<form method="get">
  Month: <input type="number" name="month" min="1" max="12" required>
  Year: <input type="number" name="year" min="2000" max="2200" required>
  <button type="submit">View report</button>
</form>
<p>Selected month: ${month}</p>
<p>Expense count: ${count}</p>
<p>Total spending (INR): ${total}</p>
</body>
</html>
