import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { JobDetailComponent } from './job-detail.component';
import { JobPosting } from './job-posting.model';

describe('JobDetailComponent', () => {
  let fixture: ComponentFixture<JobDetailComponent>;
  let component: JobDetailComponent;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  const job: JobPosting = {
    id: '11111111-1111-1111-1111-111111111111',
    title: 'Automotive QA Engineer',
    description: 'desc',
    sector: 'automotive',
    city: 'Tangier',
    contractType: 'CDI',
    status: 'LIVE',
    companyName: 'Atlas Automotive SARL',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [JobDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: job.id }) } },
        },
      ],
    });
    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the job and treats an unauthenticated visitor as guest', () => {
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush(job);

    expect(component.job()).toEqual(job);
    expect(component.applyState()).toBe('guest');
  });

  it('treats a non-candidate (employer) as wrong-role', () => {
    auth.currentUser.set({ role: 'EMPLOYER' });
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush(job);

    expect(component.applyState()).toBe('wrong-role');
  });

  it('flags missing-cv for a candidate with no CV on file', () => {
    auth.currentUser.set({ role: 'CANDIDATE' });
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush(job);
    httpMock.expectOne('/api/v1/candidates/me').flush({
      email: 'c@test.ma',
      role: 'CANDIDATE',
      fullName: 'A',
      phone: '0600000000',
      sector: 'IT',
      city: 'Rabat',
      hasCv: false,
    });

    expect(component.applyState()).toBe('missing-cv');
  });

  it('marks ready when a candidate has a CV on file', () => {
    auth.currentUser.set({ role: 'CANDIDATE' });
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush(job);
    httpMock.expectOne('/api/v1/candidates/me').flush({
      email: 'c@test.ma',
      role: 'CANDIDATE',
      fullName: 'A',
      phone: '0600000000',
      sector: 'IT',
      city: 'Rabat',
      hasCv: true,
    });

    expect(component.applyState()).toBe('ready');
  });

  it('submits an application and flags applied on success', () => {
    auth.currentUser.set({ role: 'CANDIDATE' });
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush(job);
    httpMock.expectOne('/api/v1/candidates/me').flush({
      email: 'c@test.ma',
      role: 'CANDIDATE',
      fullName: 'A',
      phone: '0600000000',
      sector: 'IT',
      city: 'Rabat',
      hasCv: true,
    });

    component.apply();
    httpMock.expectOne(`/api/v1/jobs/${job.id}/apply`).flush({});

    expect(component.applied()).toBe(true);
    expect(component.applying()).toBe(false);
  });

  it('surfaces a clear message when applying twice (409 already applied)', () => {
    auth.currentUser.set({ role: 'CANDIDATE' });
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush(job);
    httpMock.expectOne('/api/v1/candidates/me').flush({
      email: 'c@test.ma',
      role: 'CANDIDATE',
      fullName: 'A',
      phone: '0600000000',
      sector: 'IT',
      city: 'Rabat',
      hasCv: true,
    });

    component.apply();
    httpMock.expectOne(`/api/v1/jobs/${job.id}/apply`).flush('already applied', { status: 409, statusText: 'Conflict' });

    expect(component.applied()).toBe(false);
    expect(component.applyError()).toBe('You have already applied to this job.');
  });

  it('flags notFound when the job fails to load', () => {
    fixture.detectChanges();
    httpMock.expectOne(`/api/v1/jobs/${job.id}`).flush('not found', { status: 404, statusText: 'Not Found' });

    expect(component.notFound()).toBe(true);
    expect(component.loading()).toBe(false);
  });
});
