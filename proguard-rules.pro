# Room e Compose já trazem suas próprias regras de consumo (consumer rules).
# Mantemos os enums do domínio porque o Room grava o nome (name) deles no banco.
-keepclassmembers enum br.com.fiap.zelabairro.model.** { *; }
