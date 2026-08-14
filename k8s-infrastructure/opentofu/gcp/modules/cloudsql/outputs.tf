output "name" { value = google_sql_database_instance.platform.name }
output "connection_name" { value = google_sql_database_instance.platform.connection_name }
output "private_ip_address" { value = google_sql_database_instance.platform.private_ip_address }
output "database_names" { value = keys(google_sql_database.databases) }
output "user_names" { value = keys(google_sql_user.users) }
