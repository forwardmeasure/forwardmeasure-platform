output "managed_zone_name" { value = local.zone_name }
output "name_servers" { value = local.name_servers }
output "record_names" { value = { for key, record in google_dns_record_set.records : key => record.name } }
