import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { JobPosting } from './job-posting.model';

@Component({
  selector: 'app-job-search',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './job-search.component.html',
})
export class JobSearchComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  readonly jobs = signal<JobPosting[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  filters = this.fb.group({
    sector: [''],
    city: [''],
    contractType: [''],
  });

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading.set(true);
    this.error.set(null);

    let params = new HttpParams();
    const { sector, city, contractType } = this.filters.getRawValue();
    if (sector) params = params.set('sector', sector);
    if (city) params = params.set('city', city);
    if (contractType) params = params.set('contractType', contractType);

    this.http.get<JobPosting[]>('/api/v1/jobs', { params }).subscribe({
      next: (jobs) => {
        this.jobs.set(jobs);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Search failed, retry');
      },
    });
  }
}
