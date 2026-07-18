import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminMetrics } from './admin-metrics.model';

@Component({
  selector: 'app-admin-metrics',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './admin-metrics.component.html',
})
export class AdminMetricsComponent implements OnInit {
  private http = inject(HttpClient);

  readonly metrics = signal<AdminMetrics | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.http.get<AdminMetrics>('/api/v1/admin/metrics').subscribe({
      next: (metrics) => {
        this.metrics.set(metrics);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load platform metrics.');
      },
    });
  }
}
