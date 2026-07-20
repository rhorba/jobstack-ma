import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { JobPosting } from './job-posting.model';
import { PageResponse } from '../../shared/page-response.model';

const PAGE_SIZE = 20;

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
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  filters = this.fb.group({
    sector: [''],
    city: [''],
    contractType: [''],
  });

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.page.set(0);
    this.load();
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.set(this.page() + 1);
      this.load();
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.load();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    let params = new HttpParams().set('page', this.page()).set('size', PAGE_SIZE);
    const { sector, city, contractType } = this.filters.getRawValue();
    if (sector) params = params.set('sector', sector);
    if (city) params = params.set('city', city);
    if (contractType) params = params.set('contractType', contractType);

    this.http.get<PageResponse<JobPosting>>('/api/v1/jobs', { params }).subscribe({
      next: (result) => {
        this.jobs.set(result.content);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Search failed, retry');
      },
    });
  }
}
